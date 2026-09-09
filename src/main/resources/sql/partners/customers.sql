DECLARE @DESDE DATE='2025-09-09';
-- Os clientes do ERP que a conciliação precisa olhar.
--
-- QUEM ENTRA: quem teve movimento desde @DESDE, mais TODOS os inativos.
--
-- Os inativos entram sem filtro de movimento de propósito. Com o filtro, um
-- cliente que o ERP desativou há três anos nunca apareceria — e ficaria ativo
-- no KimiumHub para sempre, recebendo newsletter. São só 114 linhas a mais
-- (medido em 2026-09-09), então não há o que economizar aqui.
--
-- SEM FILTRO DE E-MAIL, e isto é deliberado. "Ativo e com e-mail" é a regra de
-- quem pode ser CRIADO, e ela mora no Java. Aplicada aqui, ela também esconderia
-- divergências e inativos de quem já existe no cadastro — e cliente sem e-mail
-- no ERP existe: 149 ativos.
--
-- RTRIM EM TODO CAMPO DE TEXTO. As colunas do TGFPAR são CHAR e vêm preenchidas
-- com espaço à direita ("COSMAR - MERCEDES JUNDIAI              "). Sem aparar,
-- nenhum e-mail casa com o regex do value object Email e TODO cliente viraria
-- impedimento. O LinhaSankhya.texto() também apara, mas não custa nada garantir
-- aqui e o SQL fica legível sozinho.
--
-- Sem CTE e sem CAST de agregado: os dois derrubam a conexão deste servidor.
SELECT PAR.CODPARC                       AS codigo,
       RTRIM(PAR.NOMEPARC)               AS nome,
       RTRIM(ISNULL(PAR.EMAIL, ''))      AS email,
       -- Vem só com dígitos, 14 no CNPJ (medido). Cabe em parceiros.documento,
       -- que é VARCHAR(14) — formatado com pontuação estouraria a coluna.
       RTRIM(ISNULL(PAR.CGC_CPF, ''))    AS documento,
       -- Aponta para o PRÓPRIO código quando o parceiro é a matriz. É daí que
       -- sai o is_matriz: Customer.isMatriz(codParceiro, codigoMatriz).
       PAR.CODPARCMATRIZ                 AS codigo_matriz,
       PAR.ATIVO                         AS ativo
  FROM TGFPAR PAR
 WHERE PAR.CLIENTE = 'S'
   -- O CODPARC 0 é o <SEM PARCEIRO> do ERP, uma linha sentinela.
   AND PAR.CODPARC > 0
   AND (PAR.ATIVO = 'N'
        OR PAR.CODPARC IN (
             SELECT CAB.CODPARC
               FROM TGFCAB CAB
              WHERE CAB.STATUSNOTA = 'L'
                AND CAB.TIPMOV IN ('V', 'D')
                AND CAB.DTNEG >= @DESDE
             UNION
             SELECT OSE.CODPARC
               FROM TCSOSE OSE
              WHERE OSE.DTFECHAMENTO >= @DESDE))
 ORDER BY PAR.CODPARC
