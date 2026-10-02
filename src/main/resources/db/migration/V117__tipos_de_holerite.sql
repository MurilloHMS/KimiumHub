-- Tipos de holerite viram cadastro.
--
-- Eram um enum no código (e outro no site): o RH pediu os holerites de PLR numa
-- manhã, e no fim do ano vêm os de férias coletivas. Agora o RH cria o tipo
-- pela tela de envio, e ele fica salvo para os próximos.
--
-- O holerite continua guardando o CÓDIGO em holerite_documento.tipo — os já
-- enviados não mudam — e ganha a FK para o cadastro. O índice único parcial da
-- V80 (funcionário, competência, tipo, sem os cancelados) segue valendo.

CREATE TABLE payslip_types (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(40)  NOT NULL,
    label      VARCHAR(60)  NOT NULL,
    sort_order INTEGER      NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    CONSTRAINT uq_payslip_types_code UNIQUE (code),
    -- O código vai para o nome do arquivo no disco: só A-Z, 0-9 e _.
    CONSTRAINT ck_payslip_types_code CHECK (code ~ '^[A-Z0-9_]+$')
);

-- "PLR" e "plr" lado a lado separariam a auditoria em dois. Acento o serviço
-- compara; aqui fica a garantia de banco para caixa e espaço.
CREATE UNIQUE INDEX ux_payslip_types_label ON payslip_types (lower(btrim(label)));

INSERT INTO payslip_types (code, label, sort_order) VALUES
    ('SALARIO',           'Salário',          10),
    ('ADIANTAMENTO',      'Adiantamento',     20),
    ('DECIMO_TERCEIRO_1', '13º — 1ª parcela', 30),
    ('DECIMO_TERCEIRO_2', '13º — 2ª parcela', 40),
    ('PLR',               'PLR',              50),
    ('FERIAS_COLETIVAS',  'Férias coletivas', 60);

-- A FK só entra se todo holerite já enviado tiver um tipo da semente. Se não
-- tiver, a migration para e diz qual — melhor do que inventar um tipo para o
-- valor desconhecido ou apagá-lo.
DO $guarda$
DECLARE
    desconhecidos TEXT;
BEGIN
    SELECT string_agg(DISTINCT h.tipo, ', ')
      INTO desconhecidos
      FROM holerite_documento h
     WHERE NOT EXISTS (SELECT 1 FROM payslip_types t WHERE t.code = h.tipo);
    IF desconhecidos IS NOT NULL THEN
        RAISE EXCEPTION 'V117: holerite com tipo fora do cadastro: %', desconhecidos;
    END IF;
END
$guarda$;

ALTER TABLE holerite_documento ALTER COLUMN tipo TYPE VARCHAR(40);
ALTER TABLE holerite_documento ALTER COLUMN tipo DROP DEFAULT;
ALTER TABLE holerite_documento
    ADD CONSTRAINT fk_holerite_documento_tipo FOREIGN KEY (tipo) REFERENCES payslip_types (code);
