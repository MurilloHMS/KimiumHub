CREATE TABLE certificate_holder(
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    cellphone VARCHAR(11) NOT NULL,
    email VARCHAR(60) NOT NULL
);
