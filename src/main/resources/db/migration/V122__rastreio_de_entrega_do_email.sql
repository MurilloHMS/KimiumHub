-- Rastreio da entrega pelo relatório do SMTP Locaweb (2026-10-07).
--
-- "Enviado" é a Locaweb ter aceitado o e-mail; "entregue" é o servidor do
-- destinatário ter aceitado. O ERP manda o cabeçalho X-SMTPLW com o
-- tracking_id, a Locaweb devolve esse valor no campo x_smtplw da API de
-- mensagens (medido com dois envios reais), e um agendador casa um com o outro.
--
-- tracking_id, e não o id da linha: o id é gerado pelo JPA só no save, e o
-- envio na hora (sendNow) envia antes de salvar. As linhas antigas ficam sem
-- tracking_id: foram enviadas sem o cabeçalho e não têm como ser casadas.
ALTER TABLE email_queue
    ADD COLUMN tracking_id   UUID,
    ADD COLUMN delivered_at  TIMESTAMP,
    ADD COLUMN bounced_at    TIMESTAMP,
    ADD COLUMN bounce_reason TEXT;

-- Único: é a chave que volta da Locaweb, e dois e-mails com a mesma marcariam
-- um como entregue pelo outro. O Postgres aceita vários NULL, que são as antigas.
CREATE UNIQUE INDEX uq_email_queue_tracking_id ON email_queue (tracking_id);

-- O agendador procura, a cada 10 minutos, só os enviados que esperam confirmação.
CREATE INDEX idx_email_queue_awaiting_delivery ON email_queue (sent_at)
    WHERE status = 'SENT' AND tracking_id IS NOT NULL AND delivered_at IS NULL AND bounced_at IS NULL;

-- ── As tentativas das linhas antigas ──────────────────────────────────────
-- Até a V121 o agendador só contava as FALHAS: o envio que dava certo chamava
-- markEmailSent(), que não somava, e o envio na hora que falhava ia a FAILED
-- sem somar. Um e-mail que saiu na primeira aparecia com 0 tentativas. O
-- código da V121 em diante soma em toda tentativa; aqui o histórico é acertado.
-- origin IS NULL separa as antigas: desde a V121 todo e-mail nasce com origem
-- (EmailQueue.of), então isto vale tenha a V121 subido antes ou junto.
UPDATE email_queue SET attempts = attempts + 1 WHERE origin IS NULL AND status = 'SENT';
UPDATE email_queue SET attempts = 1 WHERE origin IS NULL AND status = 'FAILED' AND attempts = 0;
