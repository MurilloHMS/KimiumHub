-- A permissão que a integração com o Sankhya exige.
--
-- Sem esta migration o `@PreAuthorize` do SankhyaController pede uma
-- authority que não existe no catálogo: a tela de acessos não desenha a
-- linha, ninguém consegue conceder, e o endpoint responde 403 para todo
-- mundo — inclusive para quem administra.
--
-- ESTA "TELA" NÃO TEM PÁGINA. Não existe rota no Angular para
-- `integracao/sankhya`; é uma permissão pura, usando o mecanismo de tela
-- porque é o mecanismo que este sistema tem. Isso dá concessão e revogação
-- pela tela de acessos, que é o que se queria. Só não estranhe marcar a
-- célula e nada aparecer no menu.
--
-- MÓDULO NOVO, "Integrações", em 560. Os módulos ocupam faixas contíguas e
-- Geral termina em 550, então 560 abre a faixa seguinte. Não entrou em
-- Configurações (onde se ajusta o sistema) nem em Ferramentas (telas que
-- alguém abre e usa), e um módulo próprio dá lugar para a próxima integração
-- sem renumerar nada.
--
-- FECHADA. Nenhum modelo libera. Quem vai usar é escolhido na tela de
-- acessos, uma conta de cada vez — e a conta prevista é a `consulta.sankhya`,
-- que existe só para o lote periódico entrar.
--
-- Vale lembrar o que essa célula concede: o endpoint executa SQL arbitrário
-- no ERP. A fronteira real é o usuário do Sankhya, que é só de leitura; esta
-- permissão é quem decide quem chega até ele.

INSERT INTO screens (code, label, module, sort_order) VALUES
  ('integracao/sankhya', 'Consulta ao Sankhya', 'Integrações', 560);

-- As sete permissões em todos os modelos, todas negadas.
--
-- Sete, e não só CONSULTAR, de propósito: a V85 preencheu os modelos com um
-- CROSS JOIN sobre as telas que existiam NAQUELE dia, e nada repete isso para
-- tela nova — sem estas linhas a tela de configuração não teria o que
-- desenhar. Manter as sete deixa a linha simétrica com as outras; as que não
-- significam nada aqui (ALTERAR, EXCLUIR) ficam negadas e sem uso, que é mais
-- barato que uma linha torta no meio da grade.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN (VALUES ('integracao/sankhya')) AS s(code)
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

-- As células dos usuários NÃO são criadas aqui: a `PermissionSyncService` cria
-- as que faltam no próximo boot, todas com `allowed = FALSE`, que é exatamente
-- o estado desejado. Escrevê-las aqui daria o mesmo resultado por um caminho
-- mais longo.
--
-- Quem tem a role DEVELOPER continua enxergando tudo: as authorities dele são
-- montadas a partir da tabela `screens`, então esta tela já entra sozinha — é
-- o que permite testar o endpoint antes de liberar a conta da integração.
