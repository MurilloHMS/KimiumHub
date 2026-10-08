-- A administração vira uma tela só (2026-10-08).
--
-- Usuários, o acesso de cada um e os modelos ficavam em três telas:
-- `settings/admin`, `settings/permissions/users` e
-- `settings/permissions/templates`. Agora tudo é `settings/admin`, e os
-- endpoints das duas telas de permissão passam a exigir `settings/admin`.
--
-- O risco é o mesmo da V87, ao contrário: quem configurava permissões pelas
-- duas telas antigas ficaria trancado fora da tela que as configura agora, e
-- não existe saída pela interface. Por isso a ação de `settings/admin` passa a
-- ser a SOMA (OU) das três telas, ação por ação: quem podia ALTERAR em
-- qualquer uma delas pode ALTERAR na administração. Ninguém perde nada.
--
-- O preço, aceito: quem tinha só CONFIGURAR em "Permissões por usuário" passa
-- a poder também bloquear e resetar senha, que eram CONFIGURAR de
-- `settings/admin`. Na tela única são a mesma pessoa cuidando das mesmas contas.

UPDATE user_permissions up
   SET allowed = TRUE
 WHERE up.screen_code = 'settings/admin'
   AND up.allowed = FALSE
   AND EXISTS (
       SELECT 1 FROM user_permissions antiga
        WHERE antiga.user_id = up.user_id
          AND antiga.permission = up.permission
          AND antiga.allowed = TRUE
          AND antiga.screen_code IN ('settings/permissions/users', 'settings/permissions/templates')
   );

UPDATE template_permissions tp
   SET allowed = TRUE
 WHERE tp.screen_code = 'settings/admin'
   AND tp.allowed = FALSE
   AND EXISTS (
       SELECT 1 FROM template_permissions antiga
        WHERE antiga.template_id = tp.template_id
          AND antiga.permission = tp.permission
          AND antiga.allowed = TRUE
          AND antiga.screen_code IN ('settings/permissions/users', 'settings/permissions/templates')
   );

-- As duas telas saem da grade. Ficam no catálogo, inativas, e não apagadas:
-- as células delas são o registro de quem tinha o quê antes desta migration.
UPDATE screens
   SET active = FALSE
 WHERE code IN ('settings/permissions/users', 'settings/permissions/templates');

UPDATE screens
   SET label = 'Administração'
 WHERE code = 'settings/admin';
