-- Login com digital / Face ID (WebAuthn).
--
-- O aparelho guarda a chave PRIVADA e nunca a entrega. Aqui fica só a chave
-- PÚBLICA, que serve para conferir assinaturas e para mais nada: uma cópia
-- desta tabela não abre conta nenhuma. Nenhum dado biométrico passa por aqui.
CREATE TABLE webauthn_credentials (

    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Sem o usuário, a credencial não serve para nada: apagou o usuário,
    -- apagam-se as digitais dele.
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- O id que o aparelho deu à credencial, em base64url. É por ele que o
    -- login descobre de quem é a assinatura. Até 1023 bytes pela norma, o que
    -- dá até 1364 caracteres em base64url.
    credential_id VARCHAR(1400) NOT NULL UNIQUE,

    -- A chave pública e o identificador do modelo do aparelho, no formato
    -- binário (CBOR) que a webauthn4j grava e lê de volta.
    attested_credential_data BYTEA NOT NULL,

    -- Quantas vezes o aparelho já assinou. Se um dia chegar um número MENOR
    -- que o guardado, existe uma cópia da credencial em outro lugar.
    sign_count BIGINT NOT NULL DEFAULT 0,

    -- Três marcas que o aparelho informa e a verificação confere depois:
    -- se a digital foi exigida desde o cadastro, e se a credencial pode ser
    -- sincronizada (iCloud, conta Google) e se já está.
    uv_initialized BOOLEAN NOT NULL,
    backup_eligible BOOLEAN NOT NULL,
    backup_state BOOLEAN NOT NULL,

    -- Por onde o aparelho fala: "internal" é o leitor do próprio aparelho.
    transports VARCHAR(100),

    -- O que a tela mostra: "Android · Chrome", "Windows · Edge".
    device_label VARCHAR(120) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP
);

CREATE INDEX idx_webauthn_credentials_user ON webauthn_credentials (user_id);

-- O desafio: um número aleatório que o servidor manda e o aparelho assina.
--
-- Vale uma vez e por pouco tempo. Se pudesse ser reusado, quem capturasse
-- uma resposta assinada entraria de novo com ela.
CREATE TABLE webauthn_challenges (
     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

     challenge BYTEA NOT NULL,

     purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('REGISTRATION', 'AUTHENTICATION')),

    -- No cadastro, a pessoa já está logada e o desafio é dela. No login, o
    -- servidor ainda não sabe quem é: fica vazio.
     user_id TEXT REFERENCES users(id) ON DELETE CASCADE,

     created_at TIMESTAMP NOT NULL,
     expires_at TIMESTAMP NOT NULL,
     used_at TIMESTAMP,

     CONSTRAINT ck_webauthn_challenges_registration_has_user
         CHECK (purpose <> 'REGISTRATION' OR user_id IS NOT NULL)
);

CREATE INDEX idx_webauthn_challenges_expires ON webauthn_challenges (expires_at);