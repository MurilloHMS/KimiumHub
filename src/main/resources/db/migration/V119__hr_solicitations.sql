CREATE TABLE document_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(120) NOT NULL,
    instructions TEXT,
    due_date DATE,
    status VARCHAR(10) NOT NULL CHECK(status IN ('DRAFT', 'OPEN', 'CLOSED')),
    form JSONB NOT NULL,
    template_filename VARCHAR(255),
    template_path VARCHAR(500),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE TABLE document_request_recipients (
    id UUID  PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id UUID NOT NULL REFERENCES document_requests(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES parceiros(id),
    status VARCHAR(10) NOT NULL CHECK ( status IN ('PENDING', 'SUBMITTED', 'APPROVED', 'RETURNED')),
    answers JSONB,
    added_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP,
    return_reason VARCHAR(500),
    UNIQUE (request_id, employee_id)
);

CREATE TABLE document_request_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id UUID NOT NULL REFERENCES document_request_recipients(id) ON DELETE CASCADE,
    field_key VARCHAR(60) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    replaced_at TIMESTAMP,
    employee_document_id UUID REFERENCES employee_documents(id) ON DELETE SET NULL
);

CREATE INDEX idx_document_request_recipients_employee ON document_request_recipients(employee_id);
CREATE UNIQUE INDEX ux_document_request_files_current ON document_request_files (recipient_id, field_key) WHERE replaced_at IS NULL;