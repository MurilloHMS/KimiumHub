ALTER TABLE parceiros
    ADD COLUMN vacation_balance_days INT;

CREATE TABLE vacation_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    replacement_employee_id UUID REFERENCES parceiros(id),
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_by_id UUID REFERENCES parceiros(id),
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(500)
);

CREATE INDEX idx_vacation_requests_employee_id ON vacation_requests(employee_id);
