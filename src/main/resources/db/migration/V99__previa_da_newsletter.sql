-- A prévia da newsletter: o rascunho do mês, antes de virar fila de envio.
--
-- POR QUE UMA TABELA, E NÃO MEMÓRIA. Revisar 913 clientes não é coisa de cinco
-- minutos. Sair da tela, ou a API reiniciar, jogaria fora todas as correções —
-- e a pessoa descobriria isso do pior jeito, recomeçando.
--
-- POR QUE SEPARADA DE `newsletter`. A `newsletter` é a fila de envio: linha lá
-- é e-mail que vai sair. O rascunho pode ser descartado, e misturar os dois
-- faria um mês em revisão parecer um mês pronto.

CREATE TABLE newsletter_previa (
    id                     UUID PRIMARY KEY,
    mes                    INTEGER   NOT NULL,
    ano                    INTEGER   NOT NULL,
    criado_em              TIMESTAMP NOT NULL,
    -- NULL enquanto está em revisão. Preenchido é o que fecha o mês.
    confirmado_em          TIMESTAMP,
    -- Guardado, e não recontado: é o número que a recusa do mês repetido
    -- mostra, e ele precisa dizer o que valia NAQUELE dia.
    quantidade_de_clientes INTEGER   NOT NULL DEFAULT 0,

    -- Um rascunho por período. É esta restrição que faz "pedir a prévia do
    -- mesmo mês duas vezes" devolver o mesmo rascunho, em vez de criar outro
    -- por baixo e a revisão anterior sumir sem aviso.
    CONSTRAINT uk_newsletter_previa_periodo UNIQUE (mes, ano)
);

-- Os números já juntados, um por cliente.
--
-- As colunas repetem as de `newsletter` de propósito: na confirmação uma vira a
-- outra quase campo a campo.
CREATE TABLE newsletter_previa_cliente (
    id                                     UUID PRIMARY KEY,
    previa_id                              UUID         NOT NULL,
    codigo_cliente                         VARCHAR(50)  NOT NULL,
    nome_do_cliente                        VARCHAR(255) NOT NULL,

    -- Pode ficar vazio: 31 dos 913 de junho não têm e-mail em lugar nenhum.
    email_cliente                          VARCHAR(255),
    -- Quem pediu para não receber fica marcado e fora da fila, MAS o registro
    -- é guardado: sumir com ele faria o total de clientes do mês mudar sem
    -- explicação.
    recebe_email                           BOOLEAN      NOT NULL DEFAULT TRUE,

    codigo_matriz                          VARCHAR(50),
    nome_matriz                            VARCHAR(255),

    quantidade_notas_emitidas              INTEGER      NOT NULL DEFAULT 0,
    quantidade_de_produtos                 INTEGER      NOT NULL DEFAULT 0,
    quantidade_de_litros                   DOUBLE PRECISION NOT NULL DEFAULT 0,
    quantidade_de_visitas                  INTEGER      NOT NULL DEFAULT 0,
    media_dias_atendimento                 INTEGER      NOT NULL DEFAULT 0,
    produto_em_destaque                    VARCHAR(255),

    faturamento_total                      DOUBLE PRECISION NOT NULL DEFAULT 0,
    valor_de_pecas_trocadas                DOUBLE PRECISION NOT NULL DEFAULT 0,
    valor_total_horas_por_parceiro         DOUBLE PRECISION NOT NULL DEFAULT 0,
    valor_total_cobrado_hora               DOUBLE PRECISION NOT NULL DEFAULT 0,
    mau_uso                                BOOLEAN      NOT NULL DEFAULT FALSE,
    valor_total_horas_por_parceiro_mau_uso DOUBLE PRECISION NOT NULL DEFAULT 0,
    valor_total_cobrado_hora_mau_uso       DOUBLE PRECISION NOT NULL DEFAULT 0,

    CONSTRAINT fk_previa_cliente_previa FOREIGN KEY (previa_id)
        REFERENCES newsletter_previa (id) ON DELETE CASCADE
);

CREATE INDEX idx_previa_cliente_previa ON newsletter_previa_cliente (previa_id);
CREATE INDEX idx_previa_cliente_codigo ON newsletter_previa_cliente (previa_id, codigo_cliente);

-- Uma linha por ordem de serviço.
--
-- A tabela de cliente é agregada e A CORREÇÃO É POR OS — por isso esta existe.
-- Sem ela, corrigir uma hora significaria editar o valor somado à mão, e o
-- próximo cálculo apagaria a correção sem avisar.
--
-- O texto do ERP fica guardado como veio, e é a única prova do que foi anotado:
-- "5:00 horas" tanto pode ser cinco da manhã quanto cinco horas de trabalho, e
-- só quem escreveu sabe. Foi por descartar isso em silêncio que 82 das 350 OS
-- de junho ficaram fora do valor cobrado.
CREATE TABLE newsletter_previa_os (
    id                UUID PRIMARY KEY,
    previa_id         UUID        NOT NULL,
    codigo_cliente    VARCHAR(50) NOT NULL,
    numero_os         INTEGER     NOT NULL,
    mau_uso           BOOLEAN     NOT NULL DEFAULT FALSE,

    hora_inicio_texto VARCHAR(100),
    hora_fim_texto    VARCHAR(100),
    hora_inicio       TIME,
    hora_fim          TIME,

    CONSTRAINT fk_previa_os_previa FOREIGN KEY (previa_id)
        REFERENCES newsletter_previa (id) ON DELETE CASCADE,
    CONSTRAINT uk_newsletter_previa_os UNIQUE (previa_id, numero_os)
);

CREATE INDEX idx_previa_os_cliente ON newsletter_previa_os (previa_id, codigo_cliente);


-- ═══════════════════════════════════════════════════════════════════════════
-- A tela
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Vai em Comunicação, ao lado da newsletter que já existe. Sem esta parte o
-- @PreAuthorize do controller pede uma authority que não está no catálogo: a
-- tela de acessos não desenha a linha, ninguém consegue conceder, e os quatro
-- endpoints respondem 403 para todo mundo.
--
-- Módulo Comunicação, que vai de 420 a 460 desde a V84. A 425 põe a revisão
-- logo depois de communication/newsletter (420), que é a tela com que ela se
-- confunde — e é entre as duas que o olho procura.
--
-- `active` não vai na lista porque nasce TRUE (V84), e é isso que faz a conta
-- DEVELOPER enxergar a tela assim que a migration sobe: as authorities dela
-- são montadas varrendo `screens` com active = true, sem passar por concessão.

INSERT INTO screens (code, label, module, sort_order) VALUES
  ('communication/newsletter-revisao', 'Revisão da Newsletter', 'Comunicação', 425);

-- As sete permissões em todos os modelos, todas negadas.
--
-- Sete, e não só as três que a tela usa: a V85 preencheu os modelos com um
-- CROSS JOIN sobre as telas daquele dia, e nada repete isso para tela nova —
-- sem estas linhas a tela de configuração não teria o que desenhar.
--
-- FECHADA de propósito. Confirmar dispara e-mail para a base inteira de
-- clientes; quem pode fazer isso é escolhido na tela de acessos, uma conta de
-- cada vez.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN (VALUES ('communication/newsletter-revisao')) AS s(code)
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);

-- As células dos usuários não entram aqui: a PermissionSyncService cria as que
-- faltam no próximo boot, todas negadas, que é o estado desejado.
