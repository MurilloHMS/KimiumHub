CREATE TABLE machine_movements(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movement_date DATE NOT NULL,
    quantity INT,
    machine_id UUID REFERENCES products(id) ON DELETE CASCADE
)