ALTER TABLE newsletter 
ADD COLUMN mau_uso BOOLEAN,
ADD COLUMN valor_total_cobrado_hora_mau_uso DOUBLE PRECISION,
ADD COLUMN valor_total_horas_por_parceiro_mau_uso DOUBLE PRECISION;