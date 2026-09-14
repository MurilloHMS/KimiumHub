-- Eventos da empresa, e o endereço que eles precisam para dizer onde acontecem.
--
-- Pedido em 2026-09-14: cadastrar eventos como a Poseidon Week (22 a 25), com a
-- programação de cada dia, palestrantes e o local de cada palestra. O plano está
-- em `claude/plans/eventos-2026-09-14.md`.
--
-- Quatro partes, nesta ordem:
--   1. o endereço entra no cadastro de empresas do RH;
--   2. palestrantes, eventos, palestras e a ligação palestra ↔ palestrante;
--   3. as duas telas no catálogo de permissões;
--   4. quem via o card Logos passa a ver o card Eventos, que ocupa o lugar dele.


-- ═══════════════════════════════════════════════════════════════════════════
-- 1. Endereço nas empresas
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Opcional, e em campos separados em vez de uma linha só: o mapa, o Waze e o
-- Uber procuram pelo texto montado, mas recibo, holerite e nota vão querer as
-- partes. Um endereço por empresa — matriz e fábrica com CNPJ próprio já são
-- duas linhas desta tabela.
--
-- O mesmo conjunto de colunas se repete em `company_events` e `event_talks`.
-- É o `Address` embutido no Java, e a repetição é o preço de não ter uma tabela
-- de endereços com FK que ninguém mais usaria.
ALTER TABLE companies
    ADD COLUMN address_zip_code   VARCHAR(9),
    ADD COLUMN address_street     VARCHAR(150),
    ADD COLUMN address_number     VARCHAR(20),
    ADD COLUMN address_complement VARCHAR(100),
    ADD COLUMN address_district   VARCHAR(100),
    ADD COLUMN address_city       VARCHAR(100),
    ADD COLUMN address_state      VARCHAR(2);


-- ═══════════════════════════════════════════════════════════════════════════
-- 2. Eventos
-- ═══════════════════════════════════════════════════════════════════════════

-- Cadastrado uma vez e escolhido nas palestras de qualquer evento. `company_name`
-- é texto e não FK: palestrante costuma ser de fora do grupo.
CREATE TABLE speakers (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name         VARCHAR(150) NOT NULL,
    role         VARCHAR(120),
    company_name VARCHAR(120),
    photo_url    VARCHAR(255),
    instagram    VARCHAR(100),
    linkedin     VARCHAR(100),
    website      VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100)
);

-- OS DIAS SAEM DO INTERVALO. De 22 a 25 viram quatro abas na tela, sem tabela de
-- dias; um dia sem palestra continua com a aba dele.
--
-- `published_at` NULO É RASCUNHO. Rascunho não aparece em Documentos; só no
-- cadastro. É data e não booleano para a tela poder dizer quando foi publicado.
--
-- O local é uma empresa do grupo (o endereço vem de `companies`, e mudar lá muda
-- aqui) ou um endereço digitado. Nenhum dos dois também vale: rascunho pode não
-- saber onde vai ser.
CREATE TABLE company_events (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name               VARCHAR(150) NOT NULL,
    description        TEXT,
    start_date         DATE         NOT NULL,
    end_date           DATE         NOT NULL,
    location_type      VARCHAR(20),
    company_id         UUID REFERENCES companies (id),
    place_name         VARCHAR(150),
    address_zip_code   VARCHAR(9),
    address_street     VARCHAR(150),
    address_number     VARCHAR(20),
    address_complement VARCHAR(100),
    address_district   VARCHAR(100),
    address_city       VARCHAR(100),
    address_state      VARCHAR(2),
    cover_url          VARCHAR(255),
    published_at       TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP,
    updated_by         VARCHAR(100),

    CONSTRAINT ck_company_events_period   CHECK (end_date >= start_date),
    CONSTRAINT ck_company_events_location CHECK (location_type IS NULL OR location_type IN ('COMPANY', 'ADDRESS'))
);

-- A palestra acontece no local do evento, noutra empresa do grupo, ou num
-- endereço digitado (um cliente, um kartódromo). `talk_date` + horários, sem fuso:
-- o evento é em São Paulo, como o resto do sistema (`ClockConfig.ZONA`).
--
-- Palestra sem palestrante é válida — abertura, coffee break, almoço.
--
-- ON DELETE CASCADE só a partir do evento: apagar o evento leva a programação.
-- A data dentro do intervalo do evento é regra do service, e não CHECK: o CHECK
-- não enxerga a outra tabela.
CREATE TABLE event_talks (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id           UUID         NOT NULL REFERENCES company_events (id) ON DELETE CASCADE,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    talk_date          DATE         NOT NULL,
    start_time         TIME         NOT NULL,
    end_time           TIME         NOT NULL,
    room               VARCHAR(100),
    location_type      VARCHAR(20)  NOT NULL DEFAULT 'EVENT',
    company_id         UUID REFERENCES companies (id),
    place_name         VARCHAR(150),
    address_zip_code   VARCHAR(9),
    address_street     VARCHAR(150),
    address_number     VARCHAR(20),
    address_complement VARCHAR(100),
    address_district   VARCHAR(100),
    address_city       VARCHAR(100),
    address_state      VARCHAR(2),

    CONSTRAINT ck_event_talks_time     CHECK (end_time > start_time),
    CONSTRAINT ck_event_talks_location CHECK (location_type IN ('EVENT', 'COMPANY', 'ADDRESS'))
);

CREATE INDEX ix_event_talks_schedule ON event_talks (event_id, talk_date, start_time);

-- Um ou vários palestrantes por palestra, na ordem em que foram escolhidos.
--
-- SEM CASCADE a partir do palestrante, de propósito: apagar a pessoa não pode
-- apagar a programação. O service recusa com 409 dizendo em quais eventos ela
-- está; esta FK é a trava de baixo, se alguém apagar por SQL.
CREATE TABLE event_talk_speakers (
    talk_id    UUID     NOT NULL REFERENCES event_talks (id) ON DELETE CASCADE,
    speaker_id UUID     NOT NULL REFERENCES speakers (id),
    position   SMALLINT NOT NULL,
    PRIMARY KEY (talk_id, position)
);

CREATE INDEX ix_event_talk_speakers_speaker ON event_talk_speakers (speaker_id);


-- ═══════════════════════════════════════════════════════════════════════════
-- 3. As telas
-- ═══════════════════════════════════════════════════════════════════════════
--
-- DUAS TELAS, DUAS PERMISSÕES, decisão dele: Documentos só mostra; o cadastro é
-- página própria em Comunicação.
--
--   documentos/eventos    — ver os eventos publicados (CONSULTAR)
--   communication/events  — cadastrar: CONSULTAR abre, INCLUIR cria, ALTERAR
--                           edita, publica e mexe na programação, EXCLUIR apaga
--
-- 418 é o último número livre de Documentos antes de Comunicação (420); 465 cabe
-- entre Contato (460) e Ferramentas (470).
INSERT INTO screens (code, label, module, sort_order) VALUES
  ('documentos/eventos',   'Eventos',               'Documentos',  418),
  ('communication/events', 'Cadastro de eventos',   'Comunicação', 465);

-- As sete permissões em todos os modelos, todas negadas — a V85 preencheu os
-- modelos com as telas que existiam naquele dia, e nada repete isso sozinho.
INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
SELECT t.id, s.code, p.permission, FALSE
  FROM permission_templates t
 CROSS JOIN (VALUES ('documentos/eventos'), ('communication/events')) AS s(code)
 CROSS JOIN (VALUES ('ALTERAR'), ('EXCLUIR'), ('CONSULTAR'), ('CONFIGURAR'),
                    ('INCLUIR'), ('ENVIAR'), ('BAIXAR')) AS p(permission);


-- ═══════════════════════════════════════════════════════════════════════════
-- 4. Eventos ocupa o lugar de Logos
-- ═══════════════════════════════════════════════════════════════════════════
--
-- O card Logos sai do hub de Documentos e o card Eventos entra no lugar. Sem
-- isto, quem via Logos perderia o card e não ganharia nada — e a tela nova
-- nasceria invisível para todo mundo menos a conta de desenvolvedor.
--
-- Copia SÓ o CONSULTAR, e só de quem tinha CONSULTAR em Logos: ver evento é
-- ver. Nada do cadastro é liberado aqui; `communication/events` fica fechado e
-- é concedido pela tela de acessos, uma pessoa de cada vez.
--
-- A tela de logos continua existindo (`/branding` pública e
-- `documentos/logos`), então a permissão de Logos não é tocada.
UPDATE template_permissions tp
   SET allowed = TRUE
  FROM template_permissions logos
 WHERE tp.screen_code = 'documentos/eventos'
   AND tp.permission  = 'CONSULTAR'
   AND logos.template_id = tp.template_id
   AND logos.screen_code = 'documentos/logos'
   AND logos.permission  = 'CONSULTAR'
   AND logos.allowed;

-- As células dos usuários ainda não existem: a `PermissionSyncService` cria as
-- que faltam no próximo boot, todas negadas, e não mexe nas que já existem.
-- Criando aqui as de quem via Logos, a sincronização só completa o resto.
INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
SELECT up.user_id, 'documentos/eventos', 'CONSULTAR', TRUE
  FROM user_permissions up
 WHERE up.screen_code = 'documentos/logos'
   AND up.permission  = 'CONSULTAR'
   AND up.allowed;
