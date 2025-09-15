CREATE TABLE products(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4()
    system_code VARCHAR(9) NOT NULL,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN,
    minimum_stock INTEGER
);