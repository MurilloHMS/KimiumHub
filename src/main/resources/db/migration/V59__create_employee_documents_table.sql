CREATE TABLE employee_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    title VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_employee_documents_employee_id ON employee_documents(employee_id);
