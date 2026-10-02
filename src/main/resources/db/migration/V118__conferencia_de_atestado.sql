-- Conferência de atestado: o RH confirma o recebimento ou recusa, e quem
-- enviou fica sabendo. Recusado, a pessoa reenvia — quantas vezes precisar,
-- até 30 dias depois de cada recusa.
--
-- O atestado é o MESMO: volta a "em conferência" com o arquivo novo. Cada
-- arquivo substituído vai para medical_certificate_attempts junto com a recusa
-- que levou — nada é apagado, porque a trilha do atestado é o que o RH mostra
-- se alguém perguntar por que uma falta não foi abonada.

ALTER TABLE medical_certificates
    ADD COLUMN status          VARCHAR(20),
    ADD COLUMN reviewed_by_id  UUID REFERENCES parceiros(id),
    ADD COLUMN reviewed_at     TIMESTAMP,
    ADD COLUMN review_notes    VARCHAR(500),
    -- Último reenvio: quando chegou e o que a pessoa escreveu junto.
    ADD COLUMN resubmitted_at  TIMESTAMP,
    ADD COLUMN resubmit_comment VARCHAR(500);

-- O que já tinha chegado conta como recebido: ninguém conferiu, mas também
-- ninguém vai voltar meses atrás para conferir — e mostrar tudo isso como
-- pendente enterraria os novos na Home do RH.
UPDATE medical_certificates SET status = 'RECEIVED';

ALTER TABLE medical_certificates
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ADD CONSTRAINT ck_medical_certificates_status
        CHECK (status IN ('PENDING', 'RECEIVED', 'REJECTED'));

CREATE INDEX ix_medical_certificates_status ON medical_certificates (status);

-- Uma linha por arquivo recusado e substituído, com a recusa que ele levou.
CREATE TABLE medical_certificate_attempts (
    id                UUID PRIMARY KEY,
    certificate_id    UUID         NOT NULL REFERENCES medical_certificates(id) ON DELETE CASCADE,
    submission_type   VARCHAR(10)  NOT NULL,
    confirmed_legible BOOLEAN,
    original_filename VARCHAR(255),
    storage_path      VARCHAR(500) NOT NULL,
    submitted_at      TIMESTAMP    NOT NULL,
    -- O comentário que veio com este arquivo, quando ele próprio era um reenvio.
    comment           VARCHAR(500),
    reviewed_by_id    UUID REFERENCES parceiros(id),
    reviewed_at       TIMESTAMP    NOT NULL,
    review_notes      VARCHAR(500) NOT NULL
);

CREATE INDEX ix_medical_certificate_attempts_certificate
    ON medical_certificate_attempts (certificate_id, submitted_at);
