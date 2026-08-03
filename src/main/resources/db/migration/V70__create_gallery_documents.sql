CREATE TABLE gallery_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    created_by_id VARCHAR(255) REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);