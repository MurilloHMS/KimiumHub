-- Quem criou o primeiro acesso depois da V86 e ficou trancado.
--
-- A V86 fez três coisas para todo funcionário: criou as células, liberou o
-- modelo Base, e liberou o modelo de cada role que a pessoa tinha. Quem entrou
-- depois recebeu **só a primeira** — a sincronização do boot
-- (`PermissionSyncService`) cria as combinações que faltam, sempre com
-- `allowed = FALSE`, e nada nunca liberou o Base para elas.
--
-- Resultado: a pessoa loga, não vê tela nenhuma, e a tela de acessos mostra a
-- grade inteira desmarcada. Sem erro em lugar nenhum.
--
-- Esta migration repete o que a V86 fez, uma única vez, para quem está nesse
-- estado. O provisionamento em Java passa a cobrir quem entrar daqui para
-- frente; esta aqui cobre quem já entrou.

-- O filtro é **nenhuma célula liberada**, e a escolha é deliberada.
--
-- Descarta quem já foi configurado, inclusive quem recebeu pouca coisa: liberar
-- o Base por cima de uma grade ajustada seria devolver acesso que alguém tirou
-- de propósito, e isso é pior que o problema que esta migration resolve.
--
-- O preço é conhecido: se alguém tiver sido trancado de propósito — todas as
-- células fechadas à mão — ele volta a enxergar o Base. Não há como distinguir
-- esse caso de "nunca foi provisionado" olhando só para os dados, e o Base é o
-- mínimo que qualquer funcionário tem.
CREATE TEMPORARY TABLE tmp_sem_permissao AS
SELECT u.id AS user_id
  FROM users u
 WHERE NOT EXISTS (
       SELECT 1 FROM user_roles r
        WHERE r.user_id = u.id AND r.role = 'CLIENTE'
   )
   AND EXISTS (
       SELECT 1 FROM user_permissions up WHERE up.user_id = u.id
   )
   AND NOT EXISTS (
       SELECT 1 FROM user_permissions up
        WHERE up.user_id = u.id AND up.allowed
   );

-- Todo funcionário recebe o Base.
UPDATE user_permissions up
   SET allowed = TRUE
  FROM template_permissions tp
  JOIN permission_templates t ON t.id = tp.template_id
 WHERE up.user_id IN (SELECT user_id FROM tmp_sem_permissao)
   AND t.name = 'Base'
   AND tp.screen_code = up.screen_code
   AND tp.permission  = up.permission
   AND tp.allowed;

-- E o modelo de cada role que ele tem, SOMANDO.
--
-- Quem tem VENDEDOR e ALMOXARIFADO recebe os dois e fica com a união. `USER`
-- não tem modelo de propósito: quer dizer "sem setor", e o que essa pessoa vê é
-- exatamente o Base.
UPDATE user_permissions up
   SET allowed = TRUE
  FROM user_roles r
  JOIN permission_templates t  ON t.name = r.role
  JOIN template_permissions tp ON tp.template_id = t.id
 WHERE up.user_id IN (SELECT user_id FROM tmp_sem_permissao)
   AND r.user_id = up.user_id
   AND tp.screen_code = up.screen_code
   AND tp.permission  = up.permission
   AND tp.allowed;

DROP TABLE tmp_sem_permissao;
