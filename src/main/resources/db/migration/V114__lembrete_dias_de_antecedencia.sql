-- Com quantos dias de antecedência o lembrete do evento começa (pedido de 2026-10-01).
--
-- A V113 já rodou no kimium_dev, então a coluna vem numa migration nova em vez de
-- editar a V113: o Flyway recusa subir com checksum diferente.
--
-- Lembrete ligado exige o número, como já exige a hora. Os eventos com lembrete
-- ligado antes desta migration ganham 7 dias, para a regra valer para todos.
ALTER TABLE company_events ADD COLUMN reminder_days_before INTEGER;

UPDATE company_events SET reminder_days_before = 7 WHERE reminder_enabled;

ALTER TABLE company_events
    ADD CONSTRAINT ck_company_events_reminder_days_before
        CHECK (reminder_days_before IS NULL OR reminder_days_before BETWEEN 1 AND 60),
    ADD CONSTRAINT ck_company_events_reminder_has_days
        CHECK (NOT reminder_enabled OR reminder_days_before IS NOT NULL);
