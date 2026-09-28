-- Quem recebe os relatórios do RH por e-mail. Lista mantida na tela, sem deploy:
-- a caixa do RH muda de dono, e trocar isso não pode depender de uma versão nova.
--
-- O e-mail é gravado em minúsculas pela entidade, então o UNIQUE simples basta
-- para impedir "RH@empresa" e "rh@empresa" como dois destinatários.
CREATE TABLE hr_report_recipients (
    id          UUID         PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255) NOT NULL,
    CONSTRAINT uq_hr_report_recipients_email UNIQUE (email)
);
