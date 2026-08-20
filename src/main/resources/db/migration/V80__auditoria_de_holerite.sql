-- Quem abriu e quem confirmou. Duas colunas e não uma tabela de eventos: a
-- pergunta que o RH faz é "abriu? confirmou?", uma linha por documento. Se um
-- dia precisar do histórico completo com IP e horário de cada acesso, a tabela
-- de eventos entra ao lado, sem mexer nestas.
ALTER TABLE holerite_documento
    ADD COLUMN opened_at    TIMESTAMP,
    ADD COLUMN confirmed_at TIMESTAMP;

-- Cancelamento, não exclusão: o registro nunca some. Some da tela do
-- funcionário e continua na auditoria, com quem cancelou e por quê.
ALTER TABLE holerite_documento
    ADD COLUMN canceled_at     TIMESTAMP,
    ADD COLUMN canceled_by_id  VARCHAR(255) REFERENCES users(id),
    ADD COLUMN cancel_reason   VARCHAR(300);

-- Substituição manual do arquivo, para quando o PDF subiu errado.
ALTER TABLE holerite_documento
    ADD COLUMN replaced_at    TIMESTAMP,
    ADD COLUMN replaced_by_id VARCHAR(255) REFERENCES users(id);

-- O unique passa a ignorar o que foi cancelado. Sem isto, cancelar um holerite
-- errado impediria para sempre o envio do certo: a linha cancelada continuaria
-- ocupando o trio (funcionário, competência, tipo).
DROP INDEX ux_holerite_funcionario_competencia_tipo;

CREATE UNIQUE INDEX ux_holerite_funcionario_competencia_tipo
    ON holerite_documento (employee_id, competencia, tipo)
    WHERE canceled_at IS NULL;