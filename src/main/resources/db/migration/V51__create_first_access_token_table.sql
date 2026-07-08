CREATE TABLE first_access_token (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES parceiros(id) ON DELETE CASCADE
);