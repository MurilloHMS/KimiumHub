CREATE TABLE email_queue (
     id UUID PRIMARY KEY,
     to_email VARCHAR(255),
     reply_to VARCHAR(255),
     subject VARCHAR(255),
     body TEXT,
     status VARCHAR(20),
     attempts INT DEFAULT 0,
     created_at TIMESTAMP,
     sent_at TIMESTAMP
);