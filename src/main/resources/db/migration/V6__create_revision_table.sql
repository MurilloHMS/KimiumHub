CREATE TABLE revisio(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    revision_date DATE NOT NULL,
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE
);