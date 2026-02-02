CREATE OR REPLACE FUNCTION registros_ponto_set_defaults()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.data IS NOT NULL THEN
        NEW.mes_ano := to_char(NEW.data, 'YYYY-MM');
END IF;

    IF TG_OP = 'INSERT' AND NEW.created_at IS NULL THEN
        NEW.created_at := CURRENT_TIMESTAMP;
END IF;

    NEW.updated_at := CURRENT_TIMESTAMP;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registros_ponto_before_insert
    BEFORE INSERT ON registros_ponto
    FOR EACH ROW
    EXECUTE FUNCTION registros_ponto_set_defaults();

CREATE TRIGGER trg_registros_ponto_before_update
    BEFORE UPDATE ON registros_ponto
    FOR EACH ROW
    EXECUTE FUNCTION registros_ponto_set_defaults();
