-- O catálogo das telas do ERP.
--
-- O CÓDIGO é a própria rota do Angular. A authority vai ser
-- `stock/movements:EXCLUIR`, e é isso que aparece no log, no @PreAuthorize e na
-- mensagem do 403 — com id numérico, todo diagnóstico exigiria uma consulta
-- para descobrir que a tela 37 é a de movimentações. O custo é que renomear
-- rota vira migration, mas renomear rota já quebra link salvo de qualquer jeito.
--
-- FORA DO CATÁLOGO de propósito: `home`, `notificacoes` e `unauthorized`. Essa
-- última é a tela de acesso negado: trancá-la deixaria a pessoa sem nem o aviso
-- de que foi barrada.
--
-- Os rótulos vêm do menu, com exceções onde o nome do menu colidiria no grid de
-- configuração. `documentos/rh/*` é o autoatendimento do funcionário e espelha
-- telas do RH — sem o "Meus", "Reembolsos" apareceria duas vezes e quem
-- configura escolheria no chute.
CREATE TABLE screens (
    code        VARCHAR(120) PRIMARY KEY,
    label       VARCHAR(120) NOT NULL,
    -- Agrupamento visual do grid, e nada mais. Não é FK: virar tabela obrigaria
    -- a cadastrar módulo antes de tela, para algo que só serve para ordenar.
    module      VARCHAR(60)  NOT NULL,
    -- A ordem da linha no grid. O banco não guarda ordem de inserção, então sem
    -- isto as 55 telas voltariam embaralhadas. Vai de 10 em 10 para caber tela
    -- nova no meio sem renumerar as outras: entre a 40 e a 50 entra uma 45.
    sort_order  INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_screens_module ON screens (module, sort_order);

-- A ordem é a do menu dentro de cada módulo — a que quem configura já conhece
-- de usar o sistema. Tela fora do menu vai no fim do módulo dela.
INSERT INTO screens (code, label, module, sort_order) VALUES
  ('rh/hub', 'Painel RH', 'Recursos Humanos', 10),
  ('rh/vacation-requests', 'Férias', 'Recursos Humanos', 20),
  ('rh/reimbursements', 'Reembolsos', 'Recursos Humanos', 30),
  ('rh/medical-certificates', 'Atestados', 'Recursos Humanos', 40),
  ('rh/employees', 'Funcionários', 'Recursos Humanos', 50),
  ('rh/team-overview', 'Visão de Equipe', 'Recursos Humanos', 60),
  ('rh/calendar', 'Calendário', 'Recursos Humanos', 70),
  ('rh/organizational-structure', 'Estrutura', 'Recursos Humanos', 80),
  ('rh/career-structure', 'Cargos & Níveis', 'Recursos Humanos', 90),
  ('rh/equipment-assignments', 'Equipamentos do funcionário', 'Recursos Humanos', 100),
  ('rh/calculators', 'Calculadoras', 'Recursos Humanos', 110),
  ('rh/holerit', 'Holerit', 'Recursos Humanos', 120),
  ('rh/holerit/extractor', 'Coletar Holerite', 'Recursos Humanos', 130),
  ('rh/announcements', 'Mural de Avisos', 'Recursos Humanos', 140),
  ('rh/notifications', 'Notificações', 'Recursos Humanos', 150),
  ('rh/painel-de-vagas', 'Portal de Vagas', 'Recursos Humanos', 160),
  ('rh/candidaturas', 'Candidaturas', 'Recursos Humanos', 170),
  ('stock/hub', 'Hub das Máquinas', 'Estoque', 180),
  ('stock/programacao', 'Programação', 'Estoque', 190),
  ('stock/inventory-hub', 'Hub do Estoque', 'Estoque', 200),
  ('stock/products', 'Produtos', 'Estoque', 210),
  ('stock/movements', 'Movimentações', 'Estoque', 220),
  ('stock/alerts', 'Alertas de saída', 'Estoque', 230),
  ('company/customers', 'Clientes', 'Empresa', 240),
  ('company/nfe-collector', 'Coletar Dados NFe', 'Empresa', 250),
  ('company/excel', 'Remover Senha do Excel', 'Empresa', 260),
  ('company/fuel-supply', 'Abastecimento', 'Empresa', 270),
  ('company/fuel-hub', 'Hub de Abastecimento', 'Empresa', 280),
  ('company/guide', 'Guia de Utilização', 'Empresa', 290),
  ('company/equipments', 'Equipamentos', 'Empresa', 300),
  ('finance/rent-receipt-generator', 'Gerar Recibos Locação', 'Financeiro', 310),
  ('documentos/galeria', 'Galeria', 'Documentos', 320),
  ('documentos', 'Documentos', 'Documentos', 330),
  ('documentos/rh/announcements', 'Avisos do funcionário', 'Documentos', 340),
  ('documentos/logos', 'Logos', 'Documentos', 350),
  ('documentos/holerites', 'Meus holerites', 'Documentos', 360),
  ('documentos/rh', 'Portal do funcionário', 'Documentos', 370),
  ('documentos/rh/documents', 'Meus documentos', 'Documentos', 380),
  ('documentos/rh/medical-certificates', 'Meus atestados', 'Documentos', 390),
  ('documentos/rh/reimbursements', 'Meus reembolsos', 'Documentos', 400),
  ('documentos/rh/vacation-requests', 'Minhas férias', 'Documentos', 410),
  ('communication/newsletter', 'Newsletter', 'Comunicação', 420),
  ('communication/email', 'Disparo de Emails', 'Comunicação', 430),
  ('communication/secrets', 'Comunicação Protegida', 'Comunicação', 440),
  ('communication/email-signature', 'Assinatura de Email', 'Comunicação', 450),
  ('communication/contact', 'Contato', 'Comunicação', 460),
  ('tools/pdf', 'PDF', 'Ferramentas', 470),
  ('tools/certificados', 'Certificados em lote', 'Ferramentas', 480),
  ('tools/pdf/unlock', 'Desbloquear PDF', 'Ferramentas', 490),
  ('tools/pdf/nfse-rename', 'Renomear NFS-e', 'Ferramentas', 500),
  ('faq/manager', 'Faq', 'FAQ', 510),
  ('settings/products/website', 'Produtos do site', 'Configurações', 520),
  ('settings/admin', 'Admin', 'Configurações', 530),
  ('profile-manager', 'Cartão de visita digital', 'Geral', 540),
  ('perfil', 'Meu perfil', 'Geral', 550);
