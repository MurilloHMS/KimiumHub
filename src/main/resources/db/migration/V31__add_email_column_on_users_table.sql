ALTER TABLE users
    ADD COLUMN email VARCHAR(200);

UPDATE users
SET email = CONCAT('user_', id, '@temp.com')
WHERE email IS NULL;

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT users_email_unique UNIQUE (email);