-- Lives em Eventos: o evento online, os avisos e o "Estou ciente".
--
-- A empresa passa a fazer lives semanais de comunicado (Instagram, YouTube,
-- LinkedIn, Meet...). Elas entram como evento, porque o público — empresas,
-- setores, pessoas — já existe ali.

ALTER TABLE company_events
    ADD COLUMN online_url             VARCHAR(500),
    ADD COLUMN start_time             TIME,
    ADD COLUMN end_time               TIME,
    ADD COLUMN announce_on_publish    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN announced_at           TIMESTAMP,
    ADD COLUMN notify_live_start      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN live_start_notified_at TIMESTAMP;

-- Evento que já estava publicado conta como avisado. Sem isto, despublicar e
-- publicar de novo um evento antigo mandaria "Novo evento" para todo mundo.
UPDATE company_events SET announced_at = published_at WHERE published_at IS NOT NULL;

ALTER TABLE company_events DROP CONSTRAINT ck_company_events_location;
ALTER TABLE company_events
    ADD CONSTRAINT ck_company_events_location
        CHECK (location_type IS NULL OR location_type IN ('COMPANY', 'ADDRESS', 'ONLINE')),
    -- Online sem link ou sem horário não tem o que mostrar no botão Assistir.
    ADD CONSTRAINT ck_company_events_online
        CHECK (location_type IS DISTINCT FROM 'ONLINE'
               OR (online_url IS NOT NULL AND start_time IS NOT NULL AND end_time IS NOT NULL)),
    -- O link vai num botão que todo colaborador toca sem pensar: só https.
    ADD CONSTRAINT ck_company_events_online_url
        CHECK (online_url IS NULL OR online_url LIKE 'https://%');

-- "Estou ciente". A coluna era VARCHAR(10), e ACKNOWLEDGED tem 12 letras: sem
-- alargar, a primeira confirmação daria erro ao gravar.
ALTER TABLE event_responses ALTER COLUMN answer TYPE VARCHAR(20);
ALTER TABLE event_responses DROP CONSTRAINT IF EXISTS event_responses_answer_check;
ALTER TABLE event_responses
    ADD CONSTRAINT ck_event_responses_answer CHECK (answer IN ('GOING', 'NOT_GOING', 'ACKNOWLEDGED'));
