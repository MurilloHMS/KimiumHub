ALTER TABLE parceiros
    ADD COLUMN transport_type VARCHAR(25),
    ADD COLUMN ticket_price DECIMAL(10,2),
    ADD COLUMN vehicle_km_per_liter DECIMAL(6,2),
    ADD COLUMN daily_distance_km DECIMAL(8,2);
