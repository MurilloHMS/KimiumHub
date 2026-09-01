-- O token de renovação: o que evita digitar a senha a cada duas horas.
--
-- O access token é um JWT de 2h e continua como está — sem estado, sem consulta,
-- validado só pela assinatura. Este aqui é o oposto de propósito: vive dias, e
-- por isso precisa poder ser cancelado. JWT não se cancela; linha em tabela sim.
--
-- Espelha `first_access_token`, que já é um token guardado com dono, validade e
-- marca de uso — a diferença é o tempo de vida e a rotação.
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- **DECISÃO: guarda o HASH, não o token.**
    --
    -- O `first_access_token` guarda o valor cru, e ali passa: é um convite que
    -- vale minutos e serve uma vez. Este vale dias e abre a conta inteira —
    -- quem tiver uma cópia do banco tem a sessão de todo mundo. Guardando só o
    -- SHA-256, um dump não serve para entrar, exatamente como a senha ao lado.
    --
    -- 64 caracteres é o hexadecimal de SHA-256. UNIQUE porque duas linhas com o
    -- mesmo hash seriam o mesmo token, e isso não pode acontecer.
    token_hash VARCHAR(64) NOT NULL UNIQUE,

    expires_at TIMESTAMP NOT NULL,

    -- **DECISÃO: rotação.**
    --
    -- Cada renovação marca este como usado e emite outro. Sem isso, o mesmo
    -- token vale a semana inteira e uma cópia vazada vale junto.
    --
    -- E é o que torna o vazamento DETECTÁVEL: token já usado aparecendo de novo
    -- não é engano de quem digita, é uma segunda cópia em campo. A reação é
    -- revogar todos os do usuário e obrigar login — melhor incomodar uma pessoa
    -- do que deixar duas sessões vivas com a mesma credencial.
    used_at TIMESTAMP,

    -- Logout, troca de senha, ou a detecção de reuso acima.
    revoked_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT clock_timestamp()
);

-- A consulta do fluxo: achar um token pelo hash. Já coberta pelo UNIQUE.

-- A consulta da revogação em massa — logout e detecção de reuso pedem "todos os
-- vivos desta pessoa". O índice parcial só indexa o que ainda vale, que é a
-- minoria das linhas depois de algumas semanas de uso.
CREATE INDEX idx_refresh_tokens_vivos
    ON refresh_tokens (user_id)
 WHERE used_at IS NULL AND revoked_at IS NULL;

COMMENT ON TABLE refresh_tokens IS
    'Renovação de sessão. Guarda hash, nunca o token. Rotaciona a cada uso.';
