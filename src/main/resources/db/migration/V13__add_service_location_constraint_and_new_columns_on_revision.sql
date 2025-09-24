ALTER TABLE revision
ADD COLUMN service_location_id UUID,
ADD COLUMN kilometer INT,
ADD COLUMN fiscal_note VARCHAR(10),
ADD COLUMN type VARCHAR(50),
ADD COLUMN driver_name VARCHAR(150),
ADD COLUMN observation VARCHAR(200);

ALTER TABLE revision
ADD CONSTRAINT fk_revision_service_location FOREIGN KEY (service_location_id) REFERENCES parceiros(id);