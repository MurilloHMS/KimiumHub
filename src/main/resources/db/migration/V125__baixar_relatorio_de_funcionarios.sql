-- O relatório dos funcionários sem acesso ao site (2026-10-08) é a primeira
-- coisa da tela de Funcionários que exige BAIXAR: `rh/employees:BAIXAR`.
--
-- Ação nova numa tela nasce fechada para todo mundo — a grade passa a mostrar
-- o botão "Baixar" sozinha (o catálogo lê os @PreAuthorize), mas ninguém o tem
-- marcado, e o RH inteiro levaria 403 no relatório. Quem já consulta os
-- funcionários recebe o BAIXAR aqui, nas pessoas e nos modelos: o mesmo OU,
-- ação por ação, que a V124 fez com a administração.

UPDATE user_permissions up
   SET allowed = TRUE
 WHERE up.screen_code = 'rh/employees'
   AND up.permission = 'BAIXAR'
   AND up.allowed = FALSE
   AND EXISTS (
       SELECT 1 FROM user_permissions consulta
        WHERE consulta.user_id = up.user_id
          AND consulta.screen_code = 'rh/employees'
          AND consulta.permission = 'CONSULTAR'
          AND consulta.allowed = TRUE
   );

UPDATE template_permissions tp
   SET allowed = TRUE
 WHERE tp.screen_code = 'rh/employees'
   AND tp.permission = 'BAIXAR'
   AND tp.allowed = FALSE
   AND EXISTS (
       SELECT 1 FROM template_permissions consulta
        WHERE consulta.template_id = tp.template_id
          AND consulta.screen_code = 'rh/employees'
          AND consulta.permission = 'CONSULTAR'
          AND consulta.allowed = TRUE
   );
