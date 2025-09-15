CREATE TABLE products_movements(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movement_date DATE NOT NULL,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE
);