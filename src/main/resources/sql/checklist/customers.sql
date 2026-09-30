DECLARE @ULTIMO_CLIENTE INT = 0;
-- Clientes ativos com endereço, para o vendedor achar no celular sem internet.
--
-- Paginada por CODPARC: são ~6.850 clientes, e o DbExplorer corta em 5.000
-- (burstLimit). O CODPARC 0 é o "<SEM PARCEIRO>" do sistema, ativo e cliente.
-- Colunas CHAR: RTRIM em tudo, senão todo texto volta com espaço à direita.
-- O endereço é por código: rua em TSIEND, bairro em TSIBAI, cidade em TSICID,
-- e a UF da cidade é numérica, ligada a TSIUFS.CODUF. Medido em 2026-09-29.
SELECT TOP 4000
       P.CODPARC,
       RTRIM(P.NOMEPARC)       AS NOMEPARC,
       RTRIM(P.RAZAOSOCIAL)    AS RAZAOSOCIAL,
       RTRIM(P.CGC_CPF)        AS CGC_CPF,
       P.TIPPESSOA,
       RTRIM(P.IDENTINSCESTAD) AS IE,
       RTRIM(P.TELEFONE)       AS TELEFONE,
       RTRIM(P.EMAILNFE)       AS EMAILNFE,
       RTRIM(E.TIPO)           AS TIPOLOGRADOURO,
       RTRIM(E.NOMEEND)        AS LOGRADOURO,
       RTRIM(P.NUMEND)         AS NUMERO,
       RTRIM(P.COMPLEMENTO)    AS COMPLEMENTO,
       RTRIM(B.NOMEBAI)        AS BAIRRO,
       RTRIM(C.NOMECID)        AS CIDADE,
       RTRIM(U.UF)             AS UF,
       RTRIM(P.CEP)            AS CEP,
       P.CODTAB
FROM TGFPAR P
LEFT JOIN TSIEND E ON E.CODEND = P.CODEND
LEFT JOIN TSIBAI B ON B.CODBAI = P.CODBAI
LEFT JOIN TSICID C ON C.CODCID = P.CODCID
LEFT JOIN TSIUFS U ON U.CODUF = C.UF
WHERE P.CLIENTE = 'S'
  AND P.ATIVO = 'S'
  AND P.CODPARC > @ULTIMO_CLIENTE
ORDER BY P.CODPARC
