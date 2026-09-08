DECLARE @DATA_INICIO DATE='2026-06-01'; DECLARE @DATA_FIM DATE='2026-06-30'; DECLARE @CODPARC INT=NULL;
SELECT tso.codparc AS codigo_cliente,
       tso.numos   AS numero_os,
       ISNULL(mau.MAUUSO,0) AS mau_uso,
       MAX(CASE WHEN adm.descricao='Horario de Inicio'  THEN adm.texto END) AS hora_inicio,
       MAX(CASE WHEN adm.descricao='Horario de Termino' THEN adm.texto END) AS hora_fim
  FROM ad_manutckle adm
  JOIN tcsose tso ON adm.numos = tso.numos
  LEFT JOIN (SELECT CKL.NUMOS, CASE WHEN LTRIM(RTRIM(CKE.CHK))='S' THEN 1 ELSE 0 END AS MAUUSO
               FROM AD_MANUTCKL CKL JOIN AD_MANUTCKLE CKE ON CKE.NUMOS=CKL.NUMOS
              WHERE CKE.DESCRICAO='Identificado algum mau uso?') mau ON mau.NUMOS = tso.NUMOS
 WHERE tso.dtfechamento BETWEEN @DATA_INICIO AND @DATA_FIM
   AND (@CODPARC IS NULL OR tso.codparc=@CODPARC)
 GROUP BY tso.codparc, tso.numos, mau.MAUUSO
