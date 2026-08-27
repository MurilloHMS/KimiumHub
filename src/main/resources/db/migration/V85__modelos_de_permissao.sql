-- Modelos de permissão, e o que cada um contém.
--
-- O modelo é um CARIMBO, não um pai: aplicar "Vendas" escreve as permissões no
-- usuário, e aplicar "Estoque" em cima soma. Depois disso, o que a pessoa pode
-- está escrito nela — nada é herdado em tempo de leitura. Foi a decisão que
-- resolveu o caso do vendedor que mexe em telas do estoque sem virar um grupo
-- "Vendas + Estoque".
CREATE TABLE permission_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(200),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- TODAS as combinações existem, inclusive as negadas.
--
-- A alternativa seria guardar só o que é permitido e tratar ausência como
-- negado. Fica menor, mas a tela de configuração precisaria adivinhar a
-- diferença entre "negado de propósito" e "tela que ninguém configurou ainda" —
-- e essas duas coisas ficam iguais no banco.
CREATE TABLE template_permissions (
    template_id UUID         NOT NULL REFERENCES permission_templates(id) ON DELETE CASCADE,
    screen_code VARCHAR(120) NOT NULL REFERENCES screens(code) ON DELETE CASCADE,
    permission  VARCHAR(20)  NOT NULL,
    allowed     BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (template_id, screen_code, permission),
    CONSTRAINT ck_template_permissions_permission CHECK (permission IN (
        'ALTERAR', 'EXCLUIR', 'CONSULTAR', 'CONFIGURAR', 'INCLUIR', 'ENVIAR', 'BAIXAR'
    ))
);

-- Os modelos vêm das roles de hoje, para o dia um se comportar como hoje.
-- SUPPORT entra aqui: ela protege duas rotas e nunca existiu no enum UserRole,
-- então essas telas estavam inacessíveis para todo mundo. Como modelo, funciona.
INSERT INTO permission_templates (name, description) VALUES
  ('Base', 'O que todo funcionário enxerga'),
  ('ADMIN', 'Vindo da role ADMIN'),
  ('ALMOXARIFADO', 'Vindo da role ALMOXARIFADO'),
  ('COMPRADOR', 'Vindo da role COMPRADOR'),
  ('CONTRATOS', 'Vindo da role CONTRATOS'),
  ('DESIGN', 'Vindo da role DESIGN'),
  ('FINANCEIRO', 'Vindo da role FINANCEIRO'),
  ('MARKETING', 'Vindo da role MARKETING'),
  ('RH', 'Vindo da role RH'),
  ('SUPPORT', 'Vindo da role SUPPORT'),
  ('VENDEDOR', 'Vindo da role VENDEDOR');

-- Cria as 4.235 combinações NEGADAS de uma vez.
--
-- Escrever 4.235 INSERTs deixaria o arquivo ilegível. O CROSS JOIN cria tudo
-- fechado, e os UPDATEs abaixo abrem o que cada modelo dá — que é a parte que
-- alguém vai querer auditar no olho.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN screens s
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

-- Cada modelo recebe AS SETE permissões nas telas que a role dele já abria. É o
-- comportamento de hoje: quem entra na tela hoje faz tudo dentro dela. Apertar
-- por ação é a decisão que vem depois, com o sistema no ar.

-- Base: o que todo funcionário enxerga.
--
-- São as 15 telas que hoje NÃO declaram role nenhuma no `app.routes.ts` — ou
-- seja, qualquer pessoa logada já entra nelas. Aqui isso deixa de ser silêncio
-- e passa a estar escrito.
UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'Base')
   AND screen_code IN (
        'tools/pdf',
        'tools/pdf/unlock',
        'tools/pdf/nfse-rename',
        'company/excel',
        'documentos',
        'documentos/galeria',
        'documentos/logos',
        'documentos/holerites',
        'documentos/rh',
        'documentos/rh/documents',
        'documentos/rh/medical-certificates',
        'documentos/rh/reimbursements',
        'documentos/rh/vacation-requests',
        'documentos/rh/announcements',
        'perfil'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'ADMIN')
   AND screen_code IN (
        'rh/hub',
        'rh/holerit',
        'rh/holerit/extractor',
        'rh/employees',
        'rh/organizational-structure',
        'rh/career-structure',
        'rh/vacation-requests',
        'rh/reimbursements',
        'rh/calendar',
        'rh/team-overview',
        'rh/calculators',
        'rh/equipment-assignments',
        'rh/notifications',
        'rh/announcements',
        'rh/medical-certificates',
        'rh/painel-de-vagas',
        'rh/candidaturas',
        'stock/hub',
        'stock/programacao',
        'stock/inventory-hub',
        'stock/products',
        'stock/movements',
        'stock/alerts',
        'tools/certificados',
        'company/nfe-collector',
        'company/customers',
        'company/fuel-hub',
        'company/fuel-supply',
        'company/guide',
        'company/equipments',
        'communication/newsletter',
        'communication/email',
        'communication/secrets',
        'communication/email-signature',
        'communication/contact',
        'settings/products/website',
        'settings/admin',
        'faq/manager',
        'profile-manager',
        'finance/rent-receipt-generator'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'RH')
   AND screen_code IN (
        'rh/hub',
        'rh/holerit',
        'rh/holerit/extractor',
        'rh/employees',
        'rh/organizational-structure',
        'rh/career-structure',
        'rh/vacation-requests',
        'rh/reimbursements',
        'rh/calendar',
        'rh/team-overview',
        'rh/calculators',
        'rh/equipment-assignments',
        'rh/notifications',
        'rh/announcements',
        'rh/medical-certificates',
        'rh/painel-de-vagas',
        'rh/candidaturas',
        'company/nfe-collector',
        'company/customers',
        'communication/email',
        'communication/secrets',
        'communication/email-signature'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'ALMOXARIFADO')
   AND screen_code IN (
        'stock/hub',
        'stock/programacao',
        'stock/inventory-hub',
        'stock/products',
        'stock/movements',
        'stock/alerts'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'CONTRATOS')
   AND screen_code IN (
        'stock/hub',
        'stock/programacao',
        'stock/products',
        'stock/alerts',
        'company/guide',
        'company/equipments'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'MARKETING')
   AND screen_code IN (
        'company/customers',
        'communication/newsletter',
        'communication/email',
        'communication/secrets',
        'communication/email-signature'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'DESIGN')
   AND screen_code IN (
        'company/equipments',
        'communication/email',
        'communication/email-signature',
        'settings/products/website'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'COMPRADOR')
   AND screen_code IN (
        'company/nfe-collector',
        'company/fuel-hub',
        'company/fuel-supply'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'FINANCEIRO')
   AND screen_code IN (
        'company/nfe-collector',
        'finance/rent-receipt-generator'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'SUPPORT')
   AND screen_code IN (
        'communication/email',
        'communication/contact'
   );

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'VENDEDOR')
   AND screen_code IN (
        'communication/secrets'
   );
