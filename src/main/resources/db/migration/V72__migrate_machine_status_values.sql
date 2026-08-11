ALTER TABLE products
    ALTER COLUMN machine_status TYPE VARCHAR(30);

UPDATE machine_registers SET status = 'DISPONIVEL' WHERE status = 'PRONTA';
UPDATE products SET machine_status = 'DISPONIVEL' WHERE machine_status = 'PRONTA';

UPDATE machine_registers SET status = 'REFORMA' WHERE status IN ('MANUTENCAO', 'ENTRADA');
UPDATE products SET machine_status = 'REFORMA' WHERE machine_status IN ('MANUTENCAO', 'ENTRADA');