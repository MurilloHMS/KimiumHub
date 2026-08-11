ALTER TABLE machine_registers
    ADD COLUMN created_by VARCHAR(120),
    ADD COLUMN created_at TIMESTAMP,
    ADD COLUMN updated_by VARCHAR(120),
    ADD COLUMN updated_at TIMESTAMP;

UPDATE machine_registers
SET created_by = 'Importação da planilha',
    created_at = now()
WHERE created_at IS NULL;