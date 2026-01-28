DELIMITER $$

CREATE TRIGGER before_update_registros_ponto
    BEFORE UPDATE ON registros_ponto
    FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;

    IF NEW.data IS NOT NULL THEN
        SET NEW.mes_ano = DATE_FORMAT(NEW.data, '%Y-%m');
END IF;
END$$

DELIMITER ;

DELIMITER $$

CREATE TRIGGER before_insert_registros_ponto
    BEFORE INSERT ON registros_ponto
    FOR EACH ROW
BEGIN
    IF NEW.data IS NOT NULL THEN
        SET NEW.mes_ano = DATE_FORMAT(NEW.data, '%Y-%m');
END IF;

IF NEW.created_at IS NULL THEN
        SET NEW.created_at = CURRENT_TIMESTAMP;
END IF;

    IF NEW.updated_at IS NULL THEN
        SET NEW.updated_at = CURRENT_TIMESTAMP;
END IF;
END$$

DELIMITER ;