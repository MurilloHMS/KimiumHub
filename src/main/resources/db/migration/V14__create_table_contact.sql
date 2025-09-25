CREATE TABLE contact(
    id UUID PRIMARY KEY,
    name VARCHAR(150),
    email VARCHAR(200) NOT NULL,
    contact_type VARCHAR(60),
    other_contact_type VARCHAR(100),
    message TEXT,
    business_name VARCHAR(200),
    contact_status VARCHAR(100),
    contact_date TIMESTAMP
)