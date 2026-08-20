-- Mantém o MAIS RECENTE de cada trio: reenviar era o que se fazia quando algo
-- saía errado, então a última gravação é a intenção mais atual.
DELETE FROM holerite_documento
WHERE id IN (
    SELECT id FROM (
                       SELECT id, row_number() OVER (
                           PARTITION BY employee_id, competencia, tipo ORDER BY created_at DESC
                           ) AS n
                       FROM holerite_documento
                   ) duplicados WHERE n > 1
);

CREATE UNIQUE INDEX ux_holerite_funcionario_competencia_tipo
    ON holerite_documento (employee_id, competencia, tipo);

-- Redundante depois do índice acima, que já começa por employee_id.
DROP INDEX IF EXISTS idx_holerite_employee;