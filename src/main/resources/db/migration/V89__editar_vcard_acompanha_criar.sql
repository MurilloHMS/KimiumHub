-- `perfil:ALTERAR` acompanha o `perfil:INCLUIR` da V88.
--
-- Na V88 eu tirei só o INCLUIR do modelo Base, e ficou torto: quem podia criar
-- o cartão era o vendedor, mas ALTERAR continuava ligado para a empresa toda.
-- Na tela de perfil não existe outra coisa para alterar — a seção 1 é o cadastro
-- do funcionário, só leitura. Então ALTERAR ali quer dizer "editar meu cartão",
-- e quem não pode criar não tem o que editar.
--
-- Migration nova em vez de mexer na V88: ela já rodou. Editar migration aplicada
-- é o tipo de conserto que funciona na máquina de quem edita e falha em todas as
-- outras.

UPDATE template_permissions SET allowed = FALSE
 WHERE template_id = (SELECT id FROM permission_templates WHERE name = 'Base')
   AND screen_code = 'perfil'
   AND permission  = 'ALTERAR';

UPDATE template_permissions SET allowed = TRUE
 WHERE template_id IN (
        SELECT id FROM permission_templates WHERE name IN ('VENDEDOR', 'ADMIN')
   )
   AND screen_code = 'perfil'
   AND permission  = 'ALTERAR';

-- E as pessoas de hoje, pelo mesmo motivo da V88: aplicar modelo não retroage.
UPDATE user_permissions SET allowed = FALSE
 WHERE screen_code = 'perfil'
   AND permission  = 'ALTERAR';

UPDATE user_permissions SET allowed = TRUE
 WHERE screen_code = 'perfil'
   AND permission  = 'ALTERAR'
   AND user_id IN (
        SELECT user_id FROM user_roles WHERE role IN ('VENDEDOR', 'ADMIN')
   );
