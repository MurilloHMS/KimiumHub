-- As duas telas que configuram todas as outras — e o impasse que elas criam.
--
-- Tela nova nasce fechada para todo mundo, que é o comportamento certo do
-- padrão "negar". Só que estas duas são as telas que ABREM as demais: se
-- ninguém as tiver, ninguém libera mais nada, e a feature tranca o próprio
-- dono. Não existe saída pela interface — a interface é justamente o que está
-- trancado. Por isso o ADMIN recebe as duas aqui, na migration.
INSERT INTO screens (code, label, module, sort_order) VALUES
  ('settings/permissions/templates', 'Modelos de permissão',   'Configurações', 525),
  ('settings/permissions/users',     'Permissões por usuário', 'Configurações', 527);

-- As células precisam ser criadas AQUI, e não deixadas para a sincronização do
-- boot: o Flyway roda antes do `ApplicationReadyEvent`, então na hora dos
-- UPDATEs abaixo as linhas ainda não existiriam e eles abririam zero telas.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN screens s
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
 WHERE s.code IN ('settings/permissions/templates', 'settings/permissions/users');

INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT u.id, s.code, p.permission, FALSE
  FROM users u
 CROSS JOIN screens s
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission)
 WHERE s.code IN ('settings/permissions/templates', 'settings/permissions/users')
   AND NOT EXISTS (
       SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'CLIENTE'
   );

-- O modelo ADMIN passa a conter as duas telas, para quem for carimbado com ele
-- daqui em diante já nascer conseguindo configurar.
UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'ADMIN')
   AND screen_code IN ('settings/permissions/templates', 'settings/permissions/users');

-- E quem JÁ é admin recebe agora — o carimbo não retroage, e sem esta linha o
-- UPDATE acima não abriria a tela para ninguém que já existe.
UPDATE user_permissions SET allowed = TRUE
 WHERE screen_code IN ('settings/permissions/templates', 'settings/permissions/users')
   AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'ADMIN');
