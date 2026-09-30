DECLARE @ULTIMO_PRODUTO INT = 0;
-- Produtos do checklist: os de venda e revenda (USOPROD V e R), que entram no
-- pedido e em "produtos utilizados", mais os grupos de onde a Controladoria
-- escolhe o comodato: 800002000 EQUIPAMENTOS COMODATO, 800003000 EQUIPAMENTOS
-- LAVANDERIA e 600001000 EMBALAGENS DIRETAS (galão, frasco, válvula).
--
-- IPI: TGFIPI.PERCENTUAL pelo CODIPI, e só quando TEMIPIVENDA = 'S'.
-- Embalagem: o volume alternativo (TGFVOA) de maior quantidade, quando existe —
-- 455 é GL de 7,5 KG. Muitos produtos não têm, e aí o vendedor informa.
-- O preço da tabela é por CODVOL (KG, LT); o total é embalagens × tamanho × preço.
-- Paginada por CODPROD, pelo corte de 5.000 do DbExplorer (hoje são ~1.800).
SELECT TOP 4000
       P.CODPROD,
       RTRIM(P.DESCRPROD) AS DESCRPROD,
       P.USOPROD,
       P.CODGRUPOPROD,
       RTRIM(P.CODVOL)    AS CODVOL,
       CASE WHEN P.TEMIPIVENDA = 'S' THEN ISNULL(I.PERCENTUAL, 0) ELSE 0 END AS IPI,
       (SELECT TOP 1 RTRIM(V.CODVOL) FROM TGFVOA V
         WHERE V.CODPROD = P.CODPROD AND V.DIVIDEMULTIPLICA = 'M' AND V.QUANTIDADE > 1
         ORDER BY V.QUANTIDADE DESC) AS EMBALAGEM,
       (SELECT TOP 1 V.QUANTIDADE FROM TGFVOA V
         WHERE V.CODPROD = P.CODPROD AND V.DIVIDEMULTIPLICA = 'M' AND V.QUANTIDADE > 1
         ORDER BY V.QUANTIDADE DESC) AS QTDEMBALAGEM
FROM TGFPRO P
LEFT JOIN TGFIPI I ON I.CODIPI = P.CODIPI
WHERE P.ATIVO = 'S'
  AND (P.USOPROD IN ('V', 'R') OR P.CODGRUPOPROD IN (600001000, 800002000, 800003000))
  AND P.CODPROD > @ULTIMO_PRODUTO
ORDER BY P.CODPROD
