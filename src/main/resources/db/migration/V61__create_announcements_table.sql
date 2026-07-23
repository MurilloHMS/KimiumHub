CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    published_by_id UUID NOT NULL REFERENCES parceiros(id),
    published_at TIMESTAMP NOT NULL
);
