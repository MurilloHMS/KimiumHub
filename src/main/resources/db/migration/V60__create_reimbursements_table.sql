CREATE TABLE reimbursements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    expense_date DATE NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    receipt_original_filename VARCHAR(255),
    receipt_storage_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_by_id UUID REFERENCES parceiros(id),
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(500),
    payment_date DATE,
    paid_at TIMESTAMP
);

CREATE INDEX idx_reimbursements_employee_id ON reimbursements(employee_id);
CREATE INDEX idx_reimbursements_status ON reimbursements(status);
