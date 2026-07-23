CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE position_levels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    level_order INT NOT NULL,
    position_id UUID NOT NULL REFERENCES positions(id),
    adjustment_type VARCHAR(20) NOT NULL,
    fixed_amount NUMERIC(10, 2),
    percentage_increase NUMERIC(5, 2),
    CONSTRAINT uq_position_level_order UNIQUE (position_id, level_order)
);
