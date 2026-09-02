-- Fecha as calculadoras, que uma versão anterior da V94 abriu para todo mundo.
--
-- A V94 nasceu liberando as três telas pelo modelo Base — o mesmo tratamento
-- das ferramentas de PDF. A decisão mudou depois: as calculadoras não vão para
-- todo mundo, quem usa é escolhido na tela de acessos. O arquivo da V94 foi
-- corrigido, mas o banco de desenvolvimento já tinha aplicado a versão antiga,
-- e migration aplicada não volta atrás sozinha.
--
-- Por isso esta aqui: ela desfaz o que a versão antiga liberou. Onde a V94
-- correta já rodou, não há o que desfazer e ela não muda nada — é o caso de
-- qualquer ambiente que ainda não subiu.

-- ── Os modelos ───────────────────────────────────────────────────────────────
UPDATE template_permissions SET allowed = FALSE
 WHERE screen_code IN ('documentos/calculadoras',
                       'documentos/calculadoras/combustivel',
                       'documentos/calculadoras/cmv');

-- ── As células de quem já recebeu ────────────────────────────────────────────
--
-- Fecha todas, sem tentar preservar ajuste nenhum: entre a V94 antiga e agora
-- ninguém teve tempo de configurar calculadora para ninguém — as telas nem
-- existiam antes de hoje. Preservar aqui seria preservar o próprio defeito.
UPDATE user_permissions SET allowed = FALSE
 WHERE screen_code IN ('documentos/calculadoras',
                       'documentos/calculadoras/combustivel',
                       'documentos/calculadoras/cmv');
