-- As telas de calculadora: o hub e as duas contas.
--
-- FECHADAS. Nenhum modelo libera, nenhuma célula nasce permitida — quem vai
-- usar é escolhido na tela de acessos, uma pessoa de cada vez. É a diferença
-- para as ferramentas de PDF, que entraram no modelo Base.
--
-- Duas coisas precisam acontecer aqui, e nenhuma delas libera nada:
--
--   1. as telas entram no catálogo;
--   2. os modelos ganham as combinações delas — a V85 preencheu os modelos com
--      um CROSS JOIN sobre as telas que existiam NAQUELE dia, e nada repete
--      isso para tela nova. Sem as linhas, a tela de configuração não teria o
--      que desenhar e as calculadoras não apareceriam nem para conceder.
--
-- As células dos usuários NÃO são criadas aqui de propósito: a
-- `PermissionSyncService` cria as que faltam no próximo boot, todas com
-- `allowed = FALSE`, que é exatamente o estado desejado. Escrevê-las aqui
-- daria o mesmo resultado por um caminho mais longo.

-- 415 a 417 cabem entre "Minhas férias" (410) e a Newsletter (420), que é onde
-- o módulo Documentos termina. A numeração de dez em dez existe para isto — e
-- aqui ela se gasta: uma quarta tela de Documentos vai precisar renumerar.
--
-- O hub é tela separada das duas de propósito: quem só precisa da conta de
-- combustível recebe ela sem receber a de preço de venda. Quem receber
-- qualquer uma das duas precisa receber o hub também, senão chega nela só
-- por link direto.
INSERT INTO screens (code, label, module, sort_order) VALUES
  ('documentos/calculadoras',             'Calculadoras',       'Documentos', 415),
  ('documentos/calculadoras/combustivel', 'Álcool ou gasolina', 'Documentos', 416),
  ('documentos/calculadoras/cmv',         'CMV',                'Documentos', 417);

-- As sete permissões em todos os modelos, todas negadas.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN (VALUES ('documentos/calculadoras'),
                    ('documentos/calculadoras/combustivel'),
                    ('documentos/calculadoras/cmv')) AS s(code)
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

-- Quem tem a role DEVELOPER continua enxergando tudo: as authorities dele são
-- montadas a partir da tabela `screens`, então as três telas acima já entram
-- sozinhas. É o que permite conferir as calculadoras antes de liberar alguém.
