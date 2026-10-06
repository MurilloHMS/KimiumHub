-- As duas telas das Solicitações do RH, uma de cada lado.
--
-- `rh/document-requests` é onde o RH cria, envia e confere: aberta para os
-- modelos RH e ADMIN e para quem já tem essas roles, como a V110.
--
-- `documentos/rh/requests` é onde o funcionário responde: entra no modelo Base
-- e abre para todo usuário que não é cliente, como as outras telas do portal
-- do funcionário (V85).
--
-- As células são criadas AQUI porque o Flyway roda antes da sincronização do
-- boot, e sem elas os UPDATEs abririam zero telas. E os UPDATEs são dois porque
-- modelo aplicado é cópia: mudar o modelo não muda quem já o recebeu.

INSERT INTO screens (code, label, module, sort_order) VALUES
    ('rh/document-requests', 'Solicitações do RH', 'Recursos Humanos', 57),
    ('documentos/rh/requests', 'Minhas solicitações', 'Documentos', 415);

INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
FROM permission_templates t
         CROSS JOIN (VALUES ('rh/document-requests'), ('documentos/rh/requests')) AS s(code)
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT u.id, s.code, p.permission, FALSE
FROM users u
         CROSS JOIN (VALUES ('rh/document-requests'), ('documentos/rh/requests')) AS s(code)
         CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                            ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
);

-- Lado do RH.
UPDATE template_permissions SET allowed = TRUE
WHERE screen_code = 'rh/document-requests'
  AND template_id IN (SELECT id FROM permission_templates WHERE name IN ('RH', 'ADMIN'));

UPDATE user_permissions SET allowed = TRUE
WHERE screen_code = 'rh/document-requests'
  AND user_id IN (SELECT user_id FROM user_roles WHERE role IN ('RH', 'ADMIN'));

-- Lado do funcionário: todos, menos cliente (que nem recebeu as células acima).
UPDATE template_permissions SET allowed = TRUE
WHERE screen_code = 'documentos/rh/requests'
  AND template_id = (SELECT id FROM permission_templates WHERE name = 'Base');

UPDATE user_permissions SET allowed = TRUE
WHERE screen_code = 'documentos/rh/requests';
