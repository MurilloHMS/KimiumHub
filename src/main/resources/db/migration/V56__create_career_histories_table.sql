CREATE TABLE career_histories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    position_id UUID NOT NULL REFERENCES positions(id),
    position_level_id UUID NOT NULL REFERENCES position_levels(id),
    salary NUMERIC(10, 2) NOT NULL,
    contract_type VARCHAR(10) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    effective_date DATE NOT NULL,
    notes VARCHAR(500)
);

CREATE INDEX idx_career_histories_employee_id ON career_histories(employee_id);
