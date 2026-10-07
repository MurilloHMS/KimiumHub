-- A fila de e-mail vira a única porta de saída do ERP (2026-10-07).
--
-- 1. A fila guarda de onde veio cada e-mail, o motivo da última falha e quando
--    foi a última tentativa: antes o agendador descartava a exceção.
-- 2. Os anexos ficam no disco, e esta tabela aponta para eles: o reenvio
--    funciona, e o envio manual (arquivo que só existia na requisição) também.
-- 3. Os e-mails da empresa (smtp_emails) ganham nome de exibição, ativo e
--    padrão; e cada origem escolhe de qual deles sai (email_routes).

-- ── 1. A fila ──────────────────────────────────────────────────────────────
ALTER TABLE email_queue
    ADD COLUMN origin          VARCHAR(40),
    ADD COLUMN from_name       VARCHAR(120),
    ADD COLUMN last_error      TEXT,
    ADD COLUMN last_attempt_at TIMESTAMP;

-- O agendador busca os mais antigos por status a cada minuto; a tela filtra
-- por status e por origem, sempre ordenando pela data.
CREATE INDEX idx_email_queue_status_created_at ON email_queue (status, created_at);
CREATE INDEX idx_email_queue_origin_created_at ON email_queue (origin, created_at);

-- ── 2. Os anexos ───────────────────────────────────────────────────────────
-- CASCADE: anexo sem e-mail não tem para quem ir (o arquivo no disco sai pelo serviço).
CREATE TABLE email_attachments (
    id            UUID PRIMARY KEY,
    email_id      UUID         NOT NULL REFERENCES email_queue(id) ON DELETE CASCADE,
    filename      VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    storage_path  VARCHAR(500) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);
CREATE INDEX idx_email_attachments_email_id ON email_attachments (email_id);

-- ── 3. Os remetentes ───────────────────────────────────────────────────────
-- Sem UNIQUE no e-mail aqui: não sabemos se a produção já tem repetidos, e uma
-- restrição que falha trava o boot. O serviço recusa o repetido no cadastro.
ALTER TABLE smtp_emails
    ADD COLUMN display_name VARCHAR(120),
    ADD COLUMN active       BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_default   BOOLEAN NOT NULL DEFAULT FALSE;

-- O noreply é o remetente que cinco serviços já usavam, escrito no código.
INSERT INTO smtp_emails (id, name, email, display_name, active, is_default)
SELECT uuid_generate_v4(), 'noreply', 'noreply@envios.proautokimium.com.br', 'Proauto Kimium', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM smtp_emails WHERE lower(email) = 'noreply@envios.proautokimium.com.br');

UPDATE smtp_emails SET display_name = 'Proauto Kimium' WHERE display_name IS NULL;

UPDATE smtp_emails SET is_default = TRUE
WHERE id = (SELECT id FROM smtp_emails WHERE lower(email) = 'noreply@envios.proautokimium.com.br' LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM smtp_emails WHERE is_default);

-- Uma linha por origem que tem remetente escolhido; origem sem linha usa o padrão.
CREATE TABLE email_routes (
    origin      VARCHAR(40) PRIMARY KEY,
    sender_id   UUID NOT NULL REFERENCES smtp_emails(id),
    reply_to_id UUID REFERENCES smtp_emails(id),
    updated_at  TIMESTAMP NOT NULL,
    updated_by  VARCHAR(100)
);

-- As rotas de hoje, para nada mudar no dia da subida: quem saía pelo noreply
-- continua saindo por ele, e a newsletter pelo remetente "newsletter" que ela
-- já buscava pelo nome. Relatório de reembolsos e candidaturas usavam o
-- EMAIL_FROM do ambiente: ficam sem rota e o serviço cai no mesmo EMAIL_FROM.
INSERT INTO email_routes (origin, sender_id, updated_at, updated_by)
SELECT o.origin, s.id, now(), 'V121'
FROM (VALUES ('FIRST_ACCESS'), ('PASSWORD_RESET'), ('CLIENT_INVITE'), ('TALENT_BANK'),
             ('CHECKLIST'), ('MACHINE_ALERT'), ('DOCUMENT_ALERT')) AS o(origin)
CROSS JOIN (SELECT id FROM smtp_emails WHERE lower(email) = 'noreply@envios.proautokimium.com.br' LIMIT 1) AS s;

INSERT INTO email_routes (origin, sender_id, updated_at, updated_by)
SELECT 'NEWSLETTER', id, now(), 'V121' FROM smtp_emails WHERE name = 'newsletter' LIMIT 1;

-- ── 4. As telas do desenvolvedor ───────────────────────────────────────────
-- Fechadas para todos: a conta DEVELOPER já enxerga tudo. As células nascem
-- aqui porque o Flyway roda antes da sincronização do boot (receita da V110).
INSERT INTO screens (code, label, module, sort_order) VALUES
    ('dev/email-queue', 'Fila de e-mails', 'Configurações', 535),
    ('dev/email-senders', 'Remetentes de e-mail', 'Configurações', 536);

INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
FROM permission_templates t
         CROSS JOIN (VALUES ('dev/email-queue'), ('dev/email-senders')) AS s(code)
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT u.id, s.code, p.permission, FALSE
FROM users u
         CROSS JOIN (VALUES ('dev/email-queue'), ('dev/email-senders')) AS s(code)
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
);
