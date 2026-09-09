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
BEGIN
    SELECT count(*) INTO sobrou_service FROM parceiros WHERE perfil = 'SERVICE';

    IF sobrou_service > 0 THEN
        RAISE EXCEPTION 'V101: sobraram % linhas SERVICE — o passo 2 não fez o que devia.', sobrou_service;
    END IF;
END $$;
