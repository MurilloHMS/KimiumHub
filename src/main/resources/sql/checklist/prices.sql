DECLARE @ULTIMA_TABELA INT = 0, @ULTIMO_PRODUTO INT = 0;
-- Preços vigentes de todas as tabelas: a versão (NUTAB) de maior
-- DTVIGOR <= hoje de cada CODTAB. São ~13.500 linhas em 494 tabelas.
--
-- Quais tabelas interessam (as dos clientes ativos, mais a 80) é filtrado na
-- API, com os CODTAB que o catálogo de clientes já traz. Filtrar aqui com
-- IN (SELECT ... FROM TGFPAR) levou 5–8 s por página, e com UNION a
-- conexão caiu (medido em 2026-09-30); sem o filtro, 1 s por página.
--
-- Paginada por (NUTAB, CODPROD): o DbExplorer corta em 5.000 linhas. Quem lê
-- chama de novo com a última chave até vir menos que o TOP.
SELECT TOP 4000
       A.CODTAB, E.NUTAB, E.CODPROD, E.VLRVENDA
FROM TGFEXC E
JOIN (SELECT T.CODTAB, T.NUTAB
      FROM TGFTAB T
      JOIN (SELECT CODTAB, MAX(DTVIGOR) AS VIGOR
            FROM TGFTAB
            WHERE DTVIGOR <= GETDATE()
            GROUP BY CODTAB) V ON V.CODTAB = T.CODTAB AND V.VIGOR = T.DTVIGOR) A
  ON A.NUTAB = E.NUTAB
WHERE E.NUTAB > @ULTIMA_TABELA
   OR (E.NUTAB = @ULTIMA_TABELA AND E.CODPROD > @ULTIMO_PRODUTO)
ORDER BY E.NUTAB, E.CODPROD
