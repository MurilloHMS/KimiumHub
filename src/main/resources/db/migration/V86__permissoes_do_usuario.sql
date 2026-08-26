-- O que cada pessoa pode, e é aqui que mora a verdade.
--
-- Nada é herdado em tempo de leitura. O modelo já carimbou; o que ficou escrito
-- aqui é o que vale. Por isso a resolução da permissão efetiva é uma consulta a
-- esta tabela e mais nada — sem join com modelo, sem COALESCE, sem união de
-- grupos.
--
-- `user_id` é TEXT porque `users.id` é TEXT: a entidade declara `String id` com
-- `@GeneratedValue(UUID)`, e o Hibernate gravou como texto. Declarar UUID aqui
-- faria a FK falhar na criação.
CREATE TABLE user_permissions (
    user_id     TEXT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    screen_code VARCHAR(120) NOT NULL REFERENCES screens(code) ON DELETE CASCADE,
    permission  VARCHAR(20)  NOT NULL,
    allowed     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, screen_code, permission),
    CONSTRAINT ck_user_permissions_permission CHECK (permission IN (
        'ALTERAR', 'EXCLUIR', 'CONSULTAR', 'CONFIGURAR', 'INCLUIR', 'ENVIAR', 'BAIXAR'
    ))
);

-- A consulta mais quente do sistema: ela roda no `getAuthorities()`, uma vez por
-- requisição. O índice parcial só indexa o que é permitido — que é justamente o
-- que a consulta pede, e é a menor parte da tabela.
CREATE INDEX ix_user_permissions_allowed
    ON user_permissions (user_id) WHERE allowed;

-- Quais carimbos já passaram por cada pessoa.
--
-- NÃO é vínculo vivo: apagar uma linha daqui não muda permissão nenhuma. Existe
-- para a tela do modelo poder dizer "6 usuários usaram este carimbo" e oferecer
-- reaplicar — sem isso, um modelo corrigido hoje não conserta ninguém e nem
-- avisa que deveria.
CREATE TABLE user_templates (
    user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES permission_templates(id) ON DELETE CASCADE,
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    -- Nulo quando quem aplicou foi o sistema — esta migration, ou o primeiro
    -- acesso de um funcionário novo.
    applied_by  VARCHAR(120),
    -- SOMAR liga o que o modelo dá e não desliga nada; SUBSTITUIR grava o
    -- modelo exato. É o que faz "Vendas + Estoque" funcionar sem o segundo
    -- carimbo apagar o primeiro.
    mode        VARCHAR(12) NOT NULL DEFAULT 'SOMAR',
    PRIMARY KEY (user_id, template_id),
    CONSTRAINT ck_user_templates_mode CHECK (mode IN ('SOMAR', 'SUBSTITUIR'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- O dia um: todo mundo continua enxergando o que enxerga hoje.
-- ─────────────────────────────────────────────────────────────────────────────

-- As 385 combinações de cada funcionário, todas fechadas.
--
-- Cliente fica de fora: o portal tem sessão e escopo próprios, decididos pela
-- API, e `hasRole('CLIENTE')` continua sendo o que o separa. Dar linhas de tela
-- de ERP a um cliente seria dizer que ele participa deste sistema, e ele não
-- participa.
INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT u.id, s.code, p.permission, FALSE
  FROM users u
 CROSS JOIN screens s
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
 WHERE NOT EXISTS (
       SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
 );

-- Todo funcionário recebe o Base.
UPDATE user_permissions up
   SET allowed = TRUE
  FROM template_permissions tp
  JOIN permission_templates t ON t.id = tp.template_id
 WHERE t.name = 'Base'
   AND tp.screen_code = up.screen_code
   AND tp.permission  = up.permission
   AND tp.allowed;

-- E o modelo de cada role que ele tem, SOMANDO.
--
-- Quem tem VENDEDOR e ALMOXARIFADO recebe os dois e fica com a união — sem
-- precisar de um modelo "Vendedor + Almoxarifado". Era o caso que derrubou o
-- desenho de "um grupo por pessoa".
--
-- `USER` não tem modelo de propósito: ela quer dizer "sem setor", e o que essa
-- pessoa vê é exatamente o Base.
UPDATE user_permissions up
   SET allowed = TRUE
  FROM user_roles r
  JOIN permission_templates t  ON t.name = r.role
  JOIN template_permissions tp ON tp.template_id = t.id
 WHERE r.user_id = up.user_id
   AND tp.screen_code = up.screen_code
   AND tp.permission  = up.permission
   AND tp.allowed;

-- Registra os carimbos que acabaram de acontecer, para a tela do modelo saber
-- quem reaplicar quando ele mudar.
INSERT INTO user_templates (user_id, template_id, applied_by)
SELECT u.id, t.id, 'migration V86'
  FROM users u
 CROSS JOIN permission_templates t
 WHERE NOT EXISTS (
       SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
 )
   AND (t.name = 'Base'
        OR EXISTS (SELECT 1 FROM user_roles r
                    WHERE r.user_id = u.id AND r.role = t.name));
