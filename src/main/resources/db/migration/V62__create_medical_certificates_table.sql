CREATE TABLE medical_certificates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    submission_type VARCHAR(10) NOT NULL,
    confirmed_legible BOOLEAN,
    original_filename VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    submitted_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_medical_certificates_employee_id ON medical_certificates(employee_id);
