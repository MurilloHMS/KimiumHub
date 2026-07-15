UPDATE parceiros
SET hierarquia = CASE hierarquia
     WHEN '0' THEN 'DIRETOR'
     WHEN '1' THEN 'CEO'
     WHEN '2' THEN 'SUPERVISOR'
     WHEN '3' THEN 'GERENTE'
     WHEN '4' THEN 'COORDENADOR'
     WHEN '5' THEN 'ANALISTA'
     WHEN '6' THEN 'ASSISTENTE'
     ELSE hierarquia
END
WHERE hierarquia ~ '^[0-9]+$';