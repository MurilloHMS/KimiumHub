-- O layout do Guia de Utilização sai do .jrxml e vai para o banco.
--
-- Antes, mudar uma coluna exigia Jaspersoft Studio, recompilar o .jasper e
-- deploy. Agora o designer edita na tela, salva rascunho e publica; o Jasper
-- continua desenhando o PDF, montado em tempo de execução a partir do JSON.
--
-- Uma linha por versão. Rascunho e publicado são no máximo um de cada, e quem
-- garante é o índice parcial — vale também para quem escrever direto aqui.

CREATE TABLE guide_layouts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document     TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    version      INTEGER,
    note         VARCHAR(300),
    updated_at   TIMESTAMP    NOT NULL,
    updated_by   VARCHAR(120),
    published_at TIMESTAMP,
    published_by VARCHAR(120),
    CONSTRAINT ck_guide_layouts_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    -- Rascunho não tem número; publicado e arquivado têm.
    CONSTRAINT ck_guide_layouts_version CHECK ((status = 'DRAFT') = (version IS NULL))
);

CREATE UNIQUE INDEX ux_guide_layouts_one_draft ON guide_layouts (status) WHERE status = 'DRAFT';
CREATE UNIQUE INDEX ux_guide_layouts_one_published ON guide_layouts (status) WHERE status = 'PUBLISHED';
CREATE UNIQUE INDEX ux_guide_layouts_version ON guide_layouts (version) WHERE version IS NOT NULL;

-- Imagens que o designer envia para o cabeçalho e o rodapé. No banco, e não em
-- disco: são pequenas (até 2 MB) e fazem parte do layout — uma versão antiga
-- restaurada precisa achar a imagem que usava.
CREATE TABLE guide_layout_images (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content           BYTEA        NOT NULL,
    content_type      VARCHAR(50)  NOT NULL,
    original_filename VARCHAR(200),
    width             INTEGER      NOT NULL,
    height            INTEGER      NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    created_by        VARCHAR(120)
);

-- A semente é o guia de hoje, medida por medida do guia_utilizacao.jrxml: o
-- PDF não muda no dia em que isto subir. Os testes leem o JSON daqui, entre
-- os marcadores $seed$.
--
-- A quebra de linha depois do primeiro marcador é obrigatória: grudado no {
-- do JSON, o cifrão e a chave viram a marca de variável do Flyway, que
-- recusa subir.
INSERT INTO guide_layouts (document, status, version, note, updated_at, published_at)
VALUES ($seed$
{
  "page": {
    "format": "LETTER",
    "orientation": "LANDSCAPE",
    "margins": {
      "top": 40,
      "bottom": 40,
      "left": 56,
      "right": 56
    },
    "font": "DejaVu Sans"
  },
  "header": {
    "height": 65,
    "elements": [
      {
        "type": "RECTANGLE",
        "x": 0,
        "y": 62,
        "width": 680,
        "height": 3,
        "color": "#1A3A5C"
      },
      {
        "type": "IMAGE",
        "x": 4,
        "y": 5,
        "width": 80,
        "height": 52,
        "image": "COMPANY_LOGO",
        "align": "CENTER"
      },
      {
        "type": "LINE",
        "x": 90,
        "y": 8,
        "width": 1,
        "height": 46,
        "color": "#CCCCCC"
      },
      {
        "type": "TEXT",
        "x": 98,
        "y": 12,
        "width": 152,
        "height": 40,
        "text": "GUIA DE UTILIZAÇÃO:",
        "fontSize": 14,
        "bold": true,
        "color": "#1A3A5C",
        "align": "LEFT",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "TEXT",
        "x": 256,
        "y": 12,
        "width": 200,
        "height": 40,
        "text": "{titulo}",
        "fontSize": 14,
        "bold": true,
        "color": "#00B4A0",
        "align": "LEFT",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 552,
        "y": 4,
        "width": 124,
        "height": 54,
        "image": "CUSTOMER_LOGO",
        "align": "RIGHT"
      }
    ]
  },
  "table": {
    "headerBackground": "#232E61",
    "headerColor": "#FFFFFF",
    "headerFontSize": 6.5,
    "headerHeight": 22,
    "minRowHeight": 63,
    "dividerColor": "#E0E4EA",
    "separatorColor": "#DDDDDD",
    "columns": [
      {
        "title": "PRODUTO",
        "width": 69,
        "paddingTop": 3,
        "blocks": [
          {
            "field": "NAME",
            "height": 13,
            "fontSize": 6.5,
            "bold": true,
            "uppercase": true,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          },
          {
            "field": "PHOTO",
            "height": 38,
            "fontSize": 7.5,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "CÓDIGO",
        "width": 44,
        "paddingTop": 0,
        "blocks": [
          {
            "field": "CODE",
            "height": 63,
            "fontSize": 8.0,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "COR",
        "width": 40,
        "paddingTop": 4,
        "blocks": [
          {
            "field": "COLOR_NAME",
            "height": 14,
            "fontSize": 6.5,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          },
          {
            "field": "COLOR_CIRCLE",
            "height": 18,
            "fontSize": 7.5,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "FINALIDADE",
        "width": 166,
        "paddingTop": 4,
        "blocks": [
          {
            "field": "PURPOSE",
            "height": 20,
            "fontSize": 7.5,
            "bold": true,
            "uppercase": true,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          },
          {
            "field": "DESCRIPTION",
            "height": 33,
            "fontSize": 6.5,
            "bold": false,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "TOP",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "DILUIÇÃO",
        "width": 59,
        "paddingTop": 0,
        "blocks": [
          {
            "field": "DILUTION",
            "height": 63,
            "fontSize": 7.5,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "CONCENTRAÇÃO",
        "width": 68,
        "paddingTop": 0,
        "blocks": [
          {
            "field": "CONCENTRATION",
            "height": 63,
            "fontSize": 7.5,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "LOCAL DE USO",
        "width": 76,
        "paddingTop": 3,
        "blocks": [
          {
            "field": "USAGE_AREA",
            "height": 57,
            "fontSize": 7.0,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000"
          }
        ]
      },
      {
        "title": "EQUIPAMENTOS",
        "width": 158,
        "paddingTop": 5,
        "blocks": [
          {
            "field": "EQUIPMENT_PHOTOS",
            "height": 30,
            "fontSize": 7.5,
            "bold": true,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "MIDDLE",
            "color": "#000000",
            "count": 1
          },
          {
            "field": "EQUIPMENT_NAMES",
            "height": 22,
            "fontSize": 6.5,
            "bold": false,
            "uppercase": false,
            "align": "CENTER",
            "verticalAlign": "TOP",
            "color": "#000000"
          }
        ]
      }
    ]
  },
  "footer": {
    "height": 55,
    "elements": [
      {
        "type": "RECTANGLE",
        "x": 0,
        "y": 0,
        "width": 680,
        "height": 38,
        "color": "#232E61"
      },
      {
        "type": "TEXT",
        "x": 8,
        "y": 2,
        "width": 200,
        "height": 16,
        "text": "EVITE O CONTATO COM OS OLHOS",
        "fontSize": 7.5,
        "bold": true,
        "color": "#FFFFFF",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "TEXT",
        "x": 8,
        "y": 18,
        "width": 200,
        "height": 16,
        "text": "E O CONTATO PROLONGADO COM A PELE",
        "fontSize": 7.5,
        "bold": true,
        "color": "#FFFFFF",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 332,
        "y": 2,
        "width": 32,
        "height": 32,
        "image": "PPE_MASK",
        "align": "CENTER"
      },
      {
        "type": "TEXT",
        "x": 328,
        "y": 25,
        "width": 40,
        "height": 12,
        "text": "Máscara",
        "fontSize": 6.5,
        "bold": false,
        "color": "#AAAAAA",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 387,
        "y": 2,
        "width": 32,
        "height": 32,
        "image": "PPE_GOGGLES",
        "align": "CENTER"
      },
      {
        "type": "TEXT",
        "x": 383,
        "y": 25,
        "width": 40,
        "height": 12,
        "text": "Óculos",
        "fontSize": 6.5,
        "bold": false,
        "color": "#AAAAAA",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 442,
        "y": 2,
        "width": 32,
        "height": 32,
        "image": "PPE_APRON",
        "align": "CENTER"
      },
      {
        "type": "TEXT",
        "x": 438,
        "y": 25,
        "width": 40,
        "height": 12,
        "text": "Avental",
        "fontSize": 6.5,
        "bold": false,
        "color": "#AAAAAA",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 497,
        "y": 2,
        "width": 32,
        "height": 32,
        "image": "PPE_CAP",
        "align": "CENTER"
      },
      {
        "type": "TEXT",
        "x": 493,
        "y": 25,
        "width": 40,
        "height": 12,
        "text": "Touca",
        "fontSize": 6.5,
        "bold": false,
        "color": "#AAAAAA",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 552,
        "y": 2,
        "width": 32,
        "height": 32,
        "image": "PPE_GLOVE",
        "align": "CENTER"
      },
      {
        "type": "TEXT",
        "x": 548,
        "y": 25,
        "width": 40,
        "height": 12,
        "text": "Luva",
        "fontSize": 6.5,
        "bold": false,
        "color": "#AAAAAA",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      },
      {
        "type": "IMAGE",
        "x": 607,
        "y": 2,
        "width": 32,
        "height": 32,
        "image": "PPE_BOOT",
        "align": "CENTER"
      },
      {
        "type": "TEXT",
        "x": 603,
        "y": 25,
        "width": 40,
        "height": 12,
        "text": "Bota",
        "fontSize": 6.5,
        "bold": false,
        "color": "#AAAAAA",
        "align": "CENTER",
        "verticalAlign": "MIDDLE"
      }
    ]
  }
}$seed$, 'PUBLISHED', 1, 'Layout vindo do guia_utilizacao.jrxml', NOW(), NOW());

-- Permissões em company/guide:
--   CONFIGURAR é editar e publicar o layout — Design e ADMIN.
--   INCLUIR continua sendo gerar guia para cliente — Contratos.
--
-- Até aqui CONFIGURAR não fazia nada nesta tela, e a V85 deu as sete
-- permissões a Contratos. Tirar agora não muda o dia de ninguém, e sem isso
-- Contratos publicaria layout.
UPDATE template_permissions SET allowed = FALSE
WHERE screen_code = 'company/guide' AND permission = 'CONFIGURAR'
  AND template_id IN (SELECT id FROM permission_templates WHERE name NOT IN ('ADMIN', 'DESIGN'));

UPDATE user_permissions SET allowed = FALSE
WHERE screen_code = 'company/guide' AND permission = 'CONFIGURAR'
  AND user_id NOT IN (SELECT user_id FROM user_roles WHERE role IN ('ADMIN', 'DESIGN'));

-- Design passa a ver a tela (CONSULTAR) e a editar o layout (CONFIGURAR). Não
-- ganha INCLUIR: gerar guia para cliente continua sendo de Contratos.
UPDATE template_permissions SET allowed = TRUE
WHERE screen_code = 'company/guide' AND permission IN ('CONSULTAR', 'CONFIGURAR')
  AND template_id IN (SELECT id FROM permission_templates WHERE name = 'DESIGN');

UPDATE user_permissions SET allowed = TRUE
WHERE screen_code = 'company/guide' AND permission IN ('CONSULTAR', 'CONFIGURAR')
  AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'DESIGN');
