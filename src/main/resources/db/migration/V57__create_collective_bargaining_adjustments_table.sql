CREATE TABLE collective_bargaining_adjustments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    percentage NUMERIC(5, 2) NOT NULL,
    effective_date DATE NOT NULL,
    scope VARCHAR(20) NOT NULL,
    position_id UUID REFERENCES positions(id)
);
