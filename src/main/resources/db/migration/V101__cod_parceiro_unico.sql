-- `cod_parceiro` passa a ser único em `parceiros`.
--
-- REGRA, decidida em 2026-09-09: um cliente não pode ser funcionário, e um
-- funcionário não pode ser cliente. O código é o CODPARC do Sankhya, onde ele é
-- chave da TGFPAR — duas linhas com o mesmo código são o mesmo parceiro do ERP
-- representado duas vezes.
--
-- Sem o índice, `findByCodParceiro` devolve uma das duas sem erro nenhum, e o
-- vínculo com usuário, o holerite e o histórico de carreira passam a poder cair
-- na linha errada. É o mesmo tipo de dívida que `products.system_code` já
-- cobrou uma vez.
--
-- ESTA MIGRATION APAGA LINHAS. Os números abaixo foram medidos na produção em
-- 2026-09-09, e as contagens no fim servem para conferir que ela fez o que devia:
--
--   perfil = 'CLIENTE'      7471  ->  7429   (+9 convertidas, -51 apagadas)
--   perfil = 'SERVICE'        14  ->     0
--   códigos duplicados        56  ->     0
--
-- São 56 códigos duplicados, todos com exatamente duas linhas: 51 pares
-- FUNCIONARIO+CLIENTE e 5 pares CLIENTE+SERVICE. Mais 9 linhas SERVICE sozinhas.


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 0 — a trava
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Nenhum dos 51 clientes a apagar tinha usuário do portal quando isto foi
-- medido. Mas entre a medição e o deploy alguém pode criar um, e aí o DELETE
-- falharia na FK `users.customer_id` com uma mensagem que não explica nada.
--
-- Melhor recusar dizendo o que houve. São 15 FKs apontando para `parceiros`;
-- esta é a única que um DELETE de cliente encontra.
DO $$
DECLARE com_login integer;
BEGIN
    SELECT count(*) INTO com_login
      FROM parceiros p
      JOIN users u ON u.customer_id = p.id
     WHERE p.perfil = 'CLIENTE'
       AND EXISTS (SELECT 1 FROM parceiros f
                    WHERE f.perfil = 'FUNCIONARIO'
                      AND f.cod_parceiro = p.cod_parceiro);

    IF com_login > 0 THEN
        RAISE EXCEPTION
            'V101 abortada: % cliente(s) que seriam apagados têm usuário do portal. Desvincule ou decida caso a caso antes de subir.',
            com_login;
    END IF;
END $$;


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 1 — as 9 linhas SERVICE sozinhas viram CLIENTE
-- ═══════════════════════════════════════════════════════════════════════════
--
-- SERVICE é projeto descontinuado: não existe @DiscriminatorValue para ele em
-- Java, então essas linhas não são carregáveis por nenhuma entidade. A tabela
-- `revision`, única que apontava para elas, caiu na V41 — nada as referencia.
--
-- Converter em vez de apagar sempre que der: a linha sobrevive, e nenhuma
-- referência antiga se perde. Medido que nenhuma delas colide no CNPJ com um
-- cliente existente, então o UPDATE não viola `ux_parceiros_cnpj_digits`.
--
-- **A ORDEM É ESTRUTURAL.** Este passo tem que vir ANTES do passo 2: é ele que
-- deixa o DELETE seguinte poder ser genérico. Invertidos, o DELETE leva estas 9
-- junto, e nada avisa.
UPDATE parceiros
   SET perfil = 'CLIENTE'
 WHERE perfil = 'SERVICE'
   AND cod_parceiro NOT IN (
        SELECT cod_parceiro
          FROM parceiros
         WHERE coalesce(cod_parceiro, '') <> ''
         GROUP BY cod_parceiro
        HAVING count(*) > 1);


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 2 — sobraram as 5 que duplicam um CLIENTE
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Depois do passo 1, toda linha SERVICE restante divide o código com uma linha
-- CLIENTE. Fica o cliente, que é o cadastro vivo.
DELETE FROM parceiros WHERE perfil = 'SERVICE';


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 3 — os 51 CLIENTE que duplicam um FUNCIONARIO
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Fica o funcionário: ele carrega vínculo com usuário, histórico de carreira,
-- holerite e férias. A linha de cliente do mesmo código não tem nada disso — foi
-- criada por um import que não sabia que aquele código já existia.
--
-- Consequência assumida: essas pessoas saem da base de clientes. Se alguma
-- comprar de fato, ela reaparece na próxima conciliação com o Sankhya — e aí
-- como impedimento, porque o código já é de um funcionário.
DELETE FROM parceiros c
 WHERE c.perfil = 'CLIENTE'
   AND EXISTS (SELECT 1 FROM parceiros f
                WHERE f.perfil = 'FUNCIONARIO'
                  AND f.cod_parceiro = c.cod_parceiro);


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 3.5 — `codigo_matriz` deixa de ser um double
-- ═══════════════════════════════════════════════════════════════════════════
--
-- 7149 das 7471 linhas têm o código gravado como "7.0" em vez de "7". Veio do
-- importador de Excel, que antes do commit `edec663` fazia
-- `String.valueOf(cell.getNumericCellValue())` — sem o cast para int. O
-- importador já foi corrigido; o dado nunca foi.
--
-- ISTO ESTÁ QUEBRANDO O PORTAL DO CLIENTE HOJE, EM SILÊNCIO.
-- `ClientAccessService.visibleUnits()` chama
-- `findByCodigoMatriz(customer.getCodParceiro())` — procura "7" numa coluna onde
-- está gravado "7.0", e nunca casa. Toda matriz enxerga só a si mesma e nenhuma
-- unidade do grupo aparece. O comportamento parece plausível, então ninguém
-- abriu chamado.
--
-- `cod_parceiro` foi conferido e está limpo: zero linhas com ponto. Só a coluna
-- da matriz pegou o defeito.
--
-- O padrão casa QUALQUER parte decimal, e não só `.0`. Ele e a conferência do
-- fim precisam concordar: mais estrito aqui, a conferência aborta por causa de
-- uma linha que o UPDATE se recusou a tocar. Foi exatamente o que aconteceu na
-- validação contra banco descartável.
UPDATE parceiros
   SET codigo_matriz = split_part(codigo_matriz, '.', 1)
 WHERE codigo_matriz ~ '^\d+\.\d+$';


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 3.6 — `is_matriz` passa a sair do dado, e não de um checkbox
-- ═══════════════════════════════════════════════════════════════════════════
--
-- REGRA: matriz é quando `cod_parceiro = codigo_matriz`. É o que o ERP faz
-- (CODPARCMATRIZ = CODPARC para a matriz) e o que o código já assumia:
-- `visibleUnits()` busca as unidades pelo próprio código e depois FILTRA a
-- própria linha do resultado — esse filtro só existe porque a matriz aponta
-- para si mesma.
--
-- Até agora `is_matriz` era um segundo lugar guardando a mesma verdade,
-- alimentado por um checkbox do formulário, e nada obrigava os dois a
-- concordarem. Estavam TODOS em `false`: 7471 de 7471.
--
-- Depois da normalização, 4623 viram matriz — 62% da base, contra 58% no
-- Sankhya. Os dois lados concordam, o que é a melhor evidência de que a
-- normalização acertou.
UPDATE parceiros
   SET is_matriz = (cod_parceiro = codigo_matriz)
 WHERE perfil = 'CLIENTE'
   AND coalesce(codigo_matriz, '') <> '';


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 4 — o índice
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Parcial, espelhando `ux_parceiros_cnpj_digits` da V76: existem linhas legadas
-- com `cod_parceiro` em branco, e elas não devem colidir entre si.
--
-- Sobre todos os perfis, e não sobre (cod_parceiro, perfil): é a regra do
-- negócio, não uma restrição técnica. Um código é um parceiro.
--
-- Sem CONCURRENTLY de propósito: ele não roda dentro de transação, e o Flyway
-- envolve a migration numa. São ~7,4 mil linhas — o lock é de milissegundos.
CREATE UNIQUE INDEX ux_parceiros_cod_parceiro
    ON parceiros (cod_parceiro)
 WHERE cod_parceiro IS NOT NULL AND cod_parceiro <> '';


-- ═══════════════════════════════════════════════════════════════════════════
-- Passo 5 — a conferência
-- ═══════════════════════════════════════════════════════════════════════════
--
-- O índice já garante que não sobrou duplicata: se sobrasse, o passo 4 teria
-- falhado. O que ainda pode ter dado errado é o passo 1 ter sido pulado ou
-- invertido, e as 9 linhas terem sumido em vez de virarem cliente.
DO $$
DECLARE sobrou_service integer;
        com_ponto      integer;
        matrizes       integer;
BEGIN
    SELECT count(*) INTO sobrou_service FROM parceiros WHERE perfil = 'SERVICE';

    IF sobrou_service > 0 THEN
        RAISE EXCEPTION 'V101: sobraram % linhas SERVICE — o passo 2 não fez o que devia.', sobrou_service;
    END IF;

    SELECT count(*) INTO com_ponto FROM parceiros WHERE codigo_matriz ~ '\.';

    IF com_ponto > 0 THEN
        RAISE EXCEPTION 'V101: % linhas ainda têm ponto em codigo_matriz — a normalização não pegou tudo.', com_ponto;
    END IF;

    -- Nenhuma matriz depois de normalizar significa que a regra não achou nada:
    -- ou a coluna está vazia, ou o formato é outro. Melhor recusar do que subir
    -- com o portal continuando a não mostrar unidade nenhuma.
    SELECT count(*) INTO matrizes FROM parceiros WHERE perfil = 'CLIENTE' AND is_matriz;

    IF matrizes = 0 THEN
        RAISE EXCEPTION 'V101: nenhuma matriz depois do backfill — a regra cod_parceiro = codigo_matriz não achou nada.';
    END IF;

    RAISE NOTICE 'V101 concluída: % matrizes marcadas.', matrizes;
END $$;
