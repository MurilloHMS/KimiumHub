CREATE TABLE user_roles(
  user_id TEXT NOT NULL,
  role TEXT NOT NULL,
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO user_roles(user_id, role)
SELECT id, role FROM users;

ALTER TABLE users DROP COLUMN role;