CREATE TABLE email_queue (
     id UUID PRIMARY KEY,
     to_email VARCHAR(255) NOT NULL,
     reply_to VARCHAR(255),
     from_email VARCHAR(255) NOT NULL,
     subject VARCHAR(255) NOT NULL,
     body TEXT NOT NULL,
     status VARCHAR(20) NOT NULL,
     attempts INT DEFAULT 0,
     created_at TIMESTAMP,
     sent_at TIMESTAMP
);