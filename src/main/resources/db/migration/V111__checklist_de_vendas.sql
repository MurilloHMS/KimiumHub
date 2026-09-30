-- Checklist de vendas (2026-09-30): o que o vendedor preenche na rua para
-- liberar pedido, comodato e máquina, e que a Controladoria (role CONTRATOS)
-- confere antes de lançar no Sankhya. Substitui a planilha
-- "CHECK LIST - MODELO.xlsx".

-- ─── O checklist ────────────────────────────────────────────────────────────
--
-- O id NÃO tem default: nasce no celular (UUID), porque o checklist pode ser
-- preenchido sem internet e reenviado depois de uma queda. Com o id do aparelho,
-- o reenvio cai no mesmo registro e nunca duplica.
--
-- O conteúdo é um documento de oito seções que ainda vai mudar de forma, e vai
-- inteiro em `payload`. Vira coluna só o que se filtra ou se lista: situação,
-- vendedor, cliente, datas e o total do pedido.
CREATE TABLE checklists (
    id                      UUID PRIMARY KEY,
    number                  BIGSERIAL UNIQUE,
    seller_login            VARCHAR(100) NOT NULL,
    seller_name             VARCHAR(150) NOT NULL,
    customer_code           INT,
    customer_name           VARCHAR(150) NOT NULL,
    customer_document       VARCHAR(14) NOT NULL,
    new_customer            BOOLEAN NOT NULL,
    has_order               BOOLEAN NOT NULL,
    order_total             NUMERIC(14, 2),
    status                  VARCHAR(30) NOT NULL,
    status_before_request   VARCHAR(30),
    version                 INT NOT NULL,
    payload                 JSONB NOT NULL,
    filled_offline          BOOLEAN NOT NULL DEFAULT FALSE,
    device_started_at       TIMESTAMP,
    first_submitted_at      TIMESTAMP NOT NULL,
    last_submitted_at       TIMESTAMP NOT NULL,
    reviewed_by_login       VARCHAR(100),
    reviewed_at             TIMESTAMP,
    review_notes            VARCHAR(500),
    change_reason           VARCHAR(500),
    change_requested_at     TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL
);

CREATE INDEX idx_checklists_status ON checklists(status);
CREATE INDEX idx_checklists_seller ON checklists(seller_login);

-- Cada envio aceito, inteiro: é de onde o histórico tira o "antes".
CREATE TABLE checklist_versions (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    checklist_id  UUID NOT NULL REFERENCES checklists(id) ON DELETE CASCADE,
    version       INT NOT NULL,
    payload       JSONB NOT NULL,
    submitted_by  VARCHAR(100) NOT NULL,
    submitted_at  TIMESTAMP NOT NULL,
    UNIQUE (checklist_id, version)
);

-- O que mudou de uma versão para a outra, campo a campo, calculado no
-- servidor (o celular não manda diff). A mesma ideia do histórico da
-- Programação: uma tabela, com a coluna do campo.
CREATE TABLE checklist_changes (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    checklist_id  UUID NOT NULL REFERENCES checklists(id) ON DELETE CASCADE,
    version       INT NOT NULL,
    field         VARCHAR(300) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    changed_by    VARCHAR(100) NOT NULL,
    changed_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_checklist_changes_checklist ON checklist_changes(checklist_id);

-- A linha do tempo: enviado, devolvido, alteração pedida, liberada, negada,
-- aprovado — com quem e por quê.
CREATE TABLE checklist_events (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    checklist_id  UUID NOT NULL REFERENCES checklists(id) ON DELETE CASCADE,
    type          VARCHAR(30) NOT NULL,
    version       INT NOT NULL,
    actor_login   VARCHAR(100) NOT NULL,
    actor_name    VARCHAR(150) NOT NULL,
    notes         VARCHAR(500),
    created_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_checklist_events_checklist ON checklist_events(checklist_id);

-- ─── Os cadastros que a Controladoria mantém ────────────────────────────────

-- Comunicação visual: a lista da planilha. Nome único, e desativar em vez de
-- apagar, porque checklists antigos citam o item.
CREATE TABLE checklist_visual_items (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(150) NOT NULL UNIQUE,
    sort_order  INT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO checklist_visual_items (name, sort_order) VALUES
    ('Guia de utilização geral', 10),
    ('Guia de utilização por setores', 20),
    ('Guia de utilização lavadora de louças', 30),
    ('Pop-up sanitização FLV', 40),
    ('Quadro de controle de consumo', 50),
    ('Adesivos de boas práticas', 60),
    ('Lave sempre as mãos', 70),
    ('Utilize EPI', 80),
    ('Não utilize anéis e correntes', 90),
    ('Mantenha o lixo fechado', 100),
    ('Banheiro masculino / feminino', 110),
    ('Economize água', 120),
    ('Mantenha a porta fechada', 130),
    ('Utilize touca', 140),
    ('Mantenha o ambiente limpo', 150),
    ('Não trabalhe com ferimentos', 160),
    ('Apague a luz', 170),
    ('Utilize utensílios limpos', 180),
    ('POP mãos', 190);

-- Comodato: a Controladoria escolhe produtos do Sankhya (decisão de
-- 2026-09-29). Código, nome e se está ativo vêm do ERP; aqui ficam só o nome do
-- dia a dia, que é por onde o vendedor procura, e a ordem. Começa com os 30 da
-- aba "Produtos" da planilha, com o "Uso Popular" da aba oculta.
CREATE TABLE checklist_comodato_items (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_code  INT NOT NULL UNIQUE,
    popular_name  VARCHAR(100),
    sort_order    INT NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO checklist_comodato_items (product_code, popular_name, sort_order) VALUES
    (5112, 'Dosador frigorífico', 10),
    (4752, 'Dosador condutividade máquina lavadora (esteira)', 20),
    (1389, 'Dosador secante', 30),
    (1388, 'Dosador detergente', 40),
    (4177, 'Controlador lavanderia', 50),
    (1998, 'Diluidor padrão', 60),
    (4250, 'DHD Proauto', 70),
    (3694, 'Foamer 1,3 litros', 80),
    (2921, 'Acrílico', 90),
    (1107, 'Frasco 1 litro', 100),
    (418,  'Galão de 5 litros natural', 110),
    (1144, 'Borrifador', 120),
    (1045, 'Gerador de espuma fixo', 130),
    (5148, 'Gerador fixo com engate 3/4', 140),
    (1736, 'Gerador móvel 30 litros', 150),
    (1046, 'Gerador móvel 60 litros', 160),
    (4267, 'Motobomba potência específica', 170),
    (1093, 'Motobomba padrão', 180),
    (1074, 'Dosamix', 190),
    (1075, 'Shamporizador', 200),
    (3942, 'Saboneteira comum', 210),
    (5547, 'Saboneteira Plestin', 220),
    (3947, 'Bomba de lavanderia', 230),
    (1057, 'Suporte de mangueira', 240),
    (4030, 'Suporte de foamer', 250),
    (1056, 'Suporte galão 20 litros', 260),
    (1043, 'Suporte galão 5 litros', 270),
    (1143, 'Tampa flip top', 280),
    (1145, 'Válvula pump frasco', 290),
    (1166, 'Válvula pump galão', 300);

-- ─── Telas ──────────────────────────────────────────────────────────────────
--
-- Módulo novo, Vendas, depois de Integrações (560):
--   vendas/checklist           o vendedor emite e acompanha os seus
--   vendas/checklists          a Controladoria confere todos
--   vendas/checklist-cadastros comunicação visual e comodato
--
-- Abertas por role, como na V110: as células de usuário são criadas AQUI,
-- porque o Flyway roda antes da sincronização do boot.

INSERT INTO screens (code, label, module, sort_order) VALUES
    ('vendas/checklist', 'Checklist de vendas', 'Vendas', 570),
    ('vendas/checklists', 'Checklists (Controladoria)', 'Vendas', 580),
    ('vendas/checklist-cadastros', 'Cadastros do checklist', 'Vendas', 590);

INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
FROM permission_templates t
         CROSS JOIN (VALUES ('vendas/checklist'), ('vendas/checklists'), ('vendas/checklist-cadastros')) AS s(code)
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT u.id, s.code, p.permission, FALSE
FROM users u
         CROSS JOIN (VALUES ('vendas/checklist'), ('vendas/checklists'), ('vendas/checklist-cadastros')) AS s(code)
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
);

-- O vendedor: emitir, ver os seus e baixar o comprovante.
UPDATE template_permissions SET allowed = TRUE
WHERE screen_code = 'vendas/checklist' AND permission IN ('INCLUIR', 'CONSULTAR', 'BAIXAR')
  AND template_id IN (SELECT id FROM permission_templates WHERE name = 'VENDEDOR');

UPDATE user_permissions SET allowed = TRUE
WHERE screen_code = 'vendas/checklist' AND permission IN ('INCLUIR', 'CONSULTAR', 'BAIXAR')
  AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'VENDEDOR');

-- A Controladoria (CONTRATOS) e o ADMIN: conferir e decidir (ALTERAR), baixar,
-- e manter os cadastros.
UPDATE template_permissions SET allowed = TRUE
WHERE ((screen_code = 'vendas/checklists' AND permission IN ('CONSULTAR', 'ALTERAR', 'BAIXAR'))
    OR (screen_code = 'vendas/checklist-cadastros' AND permission IN ('CONSULTAR', 'INCLUIR', 'ALTERAR', 'EXCLUIR')))
  AND template_id IN (SELECT id FROM permission_templates WHERE name IN ('CONTRATOS', 'ADMIN'));

UPDATE user_permissions SET allowed = TRUE
WHERE ((screen_code = 'vendas/checklists' AND permission IN ('CONSULTAR', 'ALTERAR', 'BAIXAR'))
    OR (screen_code = 'vendas/checklist-cadastros' AND permission IN ('CONSULTAR', 'INCLUIR', 'ALTERAR', 'EXCLUIR')))
  AND user_id IN (SELECT user_id FROM user_roles WHERE role IN ('CONTRATOS', 'ADMIN'));
