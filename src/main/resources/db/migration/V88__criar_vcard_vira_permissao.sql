-- Criar o cartão digital deixa de ser a role VENDEDOR e vira `perfil:INCLUIR`.
--
-- A tela de perfil é de todo mundo — cada um vê a própria. O que era só do
-- vendedor é **criar o cartão**, e isso estava cravado no Java como
-- `roles.contains(VENDEDOR)`. Como permissão, passa a ser configurável na tela
-- sem ninguém mexer em código.
--
-- É o primeiro caso em que a diferença entre "abrir a tela" e "fazer algo
-- dentro dela" aparece de verdade — até aqui todo modelo dava as sete de uma
-- vez, porque era o comportamento que existia.

-- O Base dava as SETE em `perfil`, o que ligaria INCLUIR para a empresa
-- inteira. Aqui ele para de dar essa uma; as outras seis continuam, e é o que
-- mantém a tela aberta para todo mundo.
UPDATE template_permissions SET allowed = FALSE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'Base')
   AND screen_code = 'perfil'
   AND permission  = 'INCLUIR';

-- O modelo VENDEDOR passa a dar. Quem for carimbado com ele daqui em diante já
-- nasce podendo criar o cartão.
UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'VENDEDOR')
   AND screen_code = 'perfil'
   AND permission  = 'INCLUIR';

-- ─────────────────────────────────────────────────────────────────────────────
-- E as pessoas que já existem. Aplicar modelo não retroage, então o dia um
-- precisa ser escrito aqui — senão todo vendedor de hoje perde o cartão.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE user_permissions SET allowed = FALSE
 WHERE screen_code = 'perfil'
   AND permission  = 'INCLUIR';

UPDATE user_permissions SET allowed = TRUE
 WHERE screen_code = 'perfil'
   AND permission  = 'INCLUIR'
   AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'VENDEDOR');

-- O ADMIN continua alcançando tudo, inclusive isto: ele precisa conseguir
-- testar a tela que configura.
UPDATE user_permissions SET allowed = TRUE
 WHERE screen_code = 'perfil'
   AND permission  = 'INCLUIR'
   AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'ADMIN');

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'ADMIN')
   AND screen_code = 'perfil'
   AND permission  = 'INCLUIR';
