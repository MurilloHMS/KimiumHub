CREATE TABLE public_secrets(
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    encrypted_content BYTEA NOT NULL,
    iv BYTEA NOT NULL,
    auth_tag BYTEA NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NULL DEFAULT now()
);

CREATE INDEX idx_public_secrets_token_hash
    ON public_secrets (token_hash);

CREATE INDEX idx_public_secrets_expires_at
    ON public_secrets (expires_at);