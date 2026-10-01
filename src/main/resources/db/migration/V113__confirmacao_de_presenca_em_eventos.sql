-- Confirmação de presença em eventos (pedido de 2026-10-01).
--
-- Quem é convidado: todos, ou a soma de empresas, setores e pessoas
-- escolhidas. A lista é calculada na hora, nunca copiada: quem entrar
-- na empresa depois do convite também passa a ser convidado.

ALTER TABLE company_events
    ADD COLUMN audience_all BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reminder_time TIME,
    ADD CONSTRAINT ck_company_events_reminder_has_time CHECK (NOT reminder_enabled OR reminder_time IS NOT NULL);

-- As empresas convidadas de um evento
CREATE TABLE company_event_audience_companies (
    event_id UUID NOT NULL REFERENCES company_events(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id),
    PRIMARY KEY (event_id, company_id)
);

-- Os departamentos convidados de um evento
CREATE TABLE company_event_audience_departments (
    event_id UUID NOT NULL REFERENCES company_events(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id),
    PRIMARY KEY (event_id, department_id)
);

-- Os funcionarios convidados de um evento
CREATE TABLE company_event_audience_employees (
    event_id UUID NOT NULL REFERENCES company_events(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    PRIMARY KEY (event_id, employee_id)
);

-- A resposta de cada convidado: uma por pessoa por evento. Pode mudar
-- até o início do evento; a primeira resposta continua registrada.
CREATE TABLE event_responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES company_events (id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES parceiros (id),
    answer VARCHAR(10) NOT NULL CHECK (answer IN ('GOING', 'NOT_GOING')),
    note VARCHAR(500),
    first_answered_at TIMESTAMP NOT NULL,
    answered_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_event_responses_event_employee UNIQUE (event_id, employee_id)
);

-- Quem abriu o evento: uma linha por pessoa, com a primeira e a última
-- vez e quantas vezes. Só conta convidado: o organizador conferindo não.
CREATE TABLE event_views (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES company_events(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    first_viewed_at TIMESTAMP NOT NULL,
    last_viewed_at TIMESTAMP NOT NULL,
    view_count INTEGER NOT NULL DEFAULT 1 CHECK (view_count >= 1),
    CONSTRAINT uq_event_views_event_employee UNIQUE (event_id, employee_id)
);

-- Um registro por evento por dia em que o lembrete saiu. A chave única
-- impede um segundo envio no mesmo dia, mesmo que o agendador rode duas
-- vezes; e o Acompanhamento mostra em que dias o lembrete foi enviado.
CREATE TABLE event_reminders_sent (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id  UUID NOT NULL REFERENCES company_events (id) ON DELETE CASCADE,
    sent_on DATE NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    recipients INTEGER NOT NULL CHECK(recipients >= 0),
    CONSTRAINT uq_event_reminders_sent_event_day UNIQUE (event_id, sent_on)
);