-- Conserta o código da tela criada pela V99.
--
-- POR QUE UMA MIGRATION NOVA, E NÃO EDITAR A V99. A V99 já foi aplicada. O
-- Flyway guarda o checksum de cada migration executada e recusa subir se o
-- arquivo mudar — a aplicação não inicia, e a mensagem fala de checksum, não
-- do que se queria consertar. Migration aplicada não se edita: corrige-se com
-- a próxima.
--
-- O QUE ESTAVA ERRADO. A V99 criou a tela como `comunicacao/newsletter-revisao`,
-- em português. O catálogo usa o prefixo em inglês desde a V84
-- (`communication/newsletter`, `communication/email`), e a rota do Angular
-- também. Com os dois códigos diferentes, o guard do site pede uma authority e
-- a API pede outra: a tela responde 403 mesmo para quem tem a permissão
-- marcada, e o sintoma parece problema de acesso em vez de erro de digitação.
--
-- E a ordem 335 caía fora da faixa de Comunicação, que vai de 420 a 460 desde
-- a V84.

-- APAGAR E RECRIAR, e não UPDATE do código.
--
-- `screens.code` é chave primária, e as duas tabelas filhas
-- (`template_permissions` e `user_permissions`) apontam para ela com
-- `ON DELETE CASCADE` e sem `ON UPDATE`. Um UPDATE no código quebraria as
-- referências; o DELETE leva as filhas junto, que é o desejado — todas elas
-- nasceram negadas na V99 e no primeiro boot depois dela.
--
-- O que se perde: qualquer concessão feita nesta tela entre a V99 e agora. São
-- minutos, e a conta DEVELOPER não depende de concessão — ela enxerga tudo que
-- está em `screens` com `active = true`.
DELETE FROM screens WHERE code = 'comunicacao/newsletter-revisao';

INSERT INTO screens (code, label, module, sort_order) VALUES
  ('communication/newsletter-revisao', 'Revisão da Newsletter', 'Comunicação', 425);

-- As sete permissões em todos os modelos, todas negadas — as mesmas que a V99
-- criou, agora sob o código certo.
--
-- FECHADA de propósito: confirmar dispara e-mail para a base inteira de
-- clientes, e quem pode fazer isso é escolhido na tela de acessos, uma conta de
-- cada vez.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN (VALUES ('communication/newsletter-revisao')) AS s(code)
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

-- As células dos usuários não entram aqui: a PermissionSyncService cria as que
-- faltam no próximo boot, todas negadas, que é o estado desejado.
