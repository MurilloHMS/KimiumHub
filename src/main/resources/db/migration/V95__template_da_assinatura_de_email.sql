-- O layout da assinatura de e-mail sai do .jrxml e passa a morar aqui.
--
-- Até hoje mudar a assinatura — mover um texto, trocar uma cor, usar outra
-- arte — era editar `templates/reports/assinatura_email/email_signature.jrxml`
-- no Jaspersoft Studio e redeployar a API. Passa a ser uma tela, e quem edita
-- é o Design e o Marketing.
--
-- O jrxml já era um canvas: uma arte de fundo de 700x300 e seis caixas de texto
-- em x/y absolutos. O que muda não é o modelo, é onde ele mora e quem alcança.

CREATE TABLE email_signature_template (
    -- UUID como todo id deste sistema, herdado de `domain.abstractions.Entity`.
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),

    -- TEXT, e não JSONB, de propósito. Nada consulta dentro do documento: ele
    -- é lido inteiro e gravado inteiro. JSONB exigiria mapeamento de tipo no
    -- Hibernate e um dialeto que o H2 dos testes não tem, para ganhar uma
    -- consulta que ninguém vai escrever.
    document   TEXT         NOT NULL,

    updated_at TIMESTAMP    NOT NULL DEFAULT now(),

    -- Quem mexeu por último. Sem isto, "a assinatura mudou e ninguém sabe por
    -- quê" não tem por onde começar.
    --
    -- Guarda o LOGIN, e não uma FK para `users(id)`. É o que o token já traz
    -- em `authentication.getName()` — com a FK, gravar exigiria uma consulta a
    -- mais só para traduzir login em id, e ler exigiria um JOIN para voltar ao
    -- nome. Coluna de auditoria se lê a olho: `murillo` responde a pergunta,
    -- um UUID não.
    updated_by VARCHAR(120),

    -- Um modelo só, e o banco é quem garante.
    --
    -- Com id UUID não dá para prender a linha por chave primária, então a
    -- trava é esta coluna: só aceita TRUE, e TRUE só cabe uma vez. Uma segunda
    -- linha esbarra no UNIQUE.
    --
    -- A alternativa seria uma coluna `ativo` e a disciplina de manter uma linha
    -- marcada — que é a mesma regra, só que sem ninguém para aplicá-la.
    singleton  BOOLEAN      NOT NULL DEFAULT TRUE UNIQUE,
    CONSTRAINT ck_email_signature_template_singleton CHECK (singleton)
);
-- ── A semente: exatamente o que o jrxml desenha hoje ─────────────────────────
--
-- As coordenadas, os tamanhos e as cores são cópia literal do
-- `email_signature.jrxml`. É o que faz o dia seguinte ao deploy sair idêntico
-- ao dia anterior — e é o que permite conferir a troca de renderizador
-- comparando com uma assinatura antiga.
--
-- `fundo` fica NULO, e isso quer dizer "a arte que vem junto com o site".
--
-- A alternativa seria semear o caminho de um arquivo e copiar o PNG para a
-- pasta de upload no deploy. Ficaria um passo manual fora do versionamento, e
-- uma tela quebrada se alguém esquecesse. Com nulo, o dia um não depende de
-- volume, de pasta nem de ninguém lembrar: a arte padrão está no bundle do
-- front, na mesma origem, e nem contamina o canvas.
INSERT INTO email_signature_template (document) VALUES ('{
    "versao": 1,
    "canvas": {
      "largura": 700,
      "altura": 300,
      "corDeFundo": "#ffffff",
      "fundo": {
        "caminho": null,
        "ajuste": "PREENCHER"
      }
    },
    "campos": [
      {
        "id": "a5dbb964-866d-4c3c-94b7-be8a4da02974",
        "chave": "nome",
        "rotulo": "Nome completo",
        "tipo": "TEXTO",
        "obrigatorio": true,
        "exemplo": "Maria Aparecida de Souza Nascimento",
        "x": 326,
        "y": 68,
        "largura": 334,
        "altura": 42,
        "fonte": "Montserrat",
        "tamanho": 28,
        "peso": 700,
        "italico": false,
        "cor": "#232E61",
        "alinhamento": "ESQUERDA",
        "alinhamentoVertical": "TOPO",
        "alturaDaLinha": 1.2,
        "estouro": "ENCOLHER",
        "ordem": 0
      },
      {
        "id": "b0db2ec1-8e03-485e-ad5e-b5130cf23836",
        "chave": "cargo",
        "rotulo": "Cargo",
        "tipo": "TEXTO",
        "obrigatorio": true,
        "exemplo": "Coordenadora de Planejamento",
        "x": 326,
        "y": 107,
        "largura": 334,
        "altura": 30,
        "fonte": "Montserrat",
        "tamanho": 18,
        "peso": 700,
        "italico": false,
        "cor": "#57C1AB",
        "alinhamento": "ESQUERDA",
        "alinhamentoVertical": "TOPO",
        "alturaDaLinha": 1.2,
        "estouro": "ENCOLHER",
        "ordem": 1
      },
      {
        "id": "acde4bff-1d1d-40d8-b018-e5bc87f127be",
        "chave": "email",
        "rotulo": "E-mail",
        "tipo": "EMAIL",
        "obrigatorio": true,
        "exemplo": "maria.nascimento@proautokimium.com.br",
        "x": 350,
        "y": 147,
        "largura": 310,
        "altura": 20,
        "fonte": "Montserrat",
        "tamanho": 11,
        "peso": 700,
        "italico": false,
        "cor": "#232E61",
        "alinhamento": "ESQUERDA",
        "alinhamentoVertical": "TOPO",
        "alturaDaLinha": 1.2,
        "estouro": "ENCOLHER",
        "ordem": 2
      },
      {
        "id": "d5b0be80-50fa-4770-84e8-050ebdbd3425",
        "chave": "celular",
        "rotulo": "Celular",
        "tipo": "TELEFONE",
        "obrigatorio": true,
        "exemplo": "(11) 95778-2766",
        "x": 348,
        "y": 179,
        "largura": 100,
        "altura": 20,
        "fonte": "Montserrat",
        "tamanho": 9,
        "peso": 700,
        "italico": false,
        "cor": "#232E61",
        "alinhamento": "ESQUERDA",
        "alinhamentoVertical": "MEIO",
        "alturaDaLinha": 1.2,
        "estouro": "ENCOLHER",
        "ordem": 3
      },
      {
        "id": "95915337-854f-4174-af6f-8ddcbf449445",
        "chave": "whatsapp",
        "rotulo": "WhatsApp",
        "tipo": "TELEFONE",
        "obrigatorio": true,
        "exemplo": "(11) 95778-2766",
        "x": 465,
        "y": 179,
        "largura": 130,
        "altura": 20,
        "fonte": "Montserrat",
        "tamanho": 9,
        "peso": 700,
        "italico": false,
        "cor": "#232E61",
        "alinhamento": "ESQUERDA",
        "alinhamentoVertical": "MEIO",
        "alturaDaLinha": 1.2,
        "estouro": "ENCOLHER",
        "ordem": 4
      },
      {
        "id": "4bf8d33c-992e-46d6-8964-5eca9985b280",
        "chave": "site",
        "rotulo": "Site",
        "tipo": "URL",
        "obrigatorio": false,
        "exemplo": "www.proautokimium.com.br",
        "x": 350,
        "y": 212,
        "largura": 310,
        "altura": 23,
        "fonte": "Montserrat",
        "tamanho": 11,
        "peso": 700,
        "italico": false,
        "cor": "#232E61",
        "alinhamento": "ESQUERDA",
        "alinhamentoVertical": "MEIO",
        "alturaDaLinha": 1.2,
        "estouro": "ENCOLHER",
        "ordem": 5
      }
    ]
  }');

-- ── A permissão que o dia um precisa, e que passaria batido ──────────────────
--
-- Hoje quem gera assinatura tem `communication/email-signature:INCLUIR` e
-- recebe um PNG pronto: nada nessa tela nunca fez um GET.
--
-- Depois desta mudança a tela LÊ o template antes de desenhar qualquer coisa, e
-- essa leitura pede CONSULTAR. Quem tiver INCLUIR sem CONSULTAR abre a tela,
-- toma 403 numa chamada que não existia ontem, e vê um formulário vazio sem
-- explicação nenhuma.
--
-- Os modelos já dão as sete permissões nessa tela (V85), então isto alcança
-- quem foi configurado na mão desde então — que é justamente quem ninguém
-- lembraria de conferir.
--
-- SÓ `CONSULTAR`. `CONFIGURAR` — editar o layout — fica de fora de propósito:
-- ligá-la aqui daria edição de layout a quem alguém limitou a gerar, de
-- propósito. É a mesma linha que a V92 traçou ao recusar liberar o Base por
-- cima de uma grade ajustada. Quem vai editar, ele marca na tela de acessos.
UPDATE user_permissions SET allowed = TRUE
 WHERE screen_code = 'communication/email-signature'
   AND permission = 'CONSULTAR'
   AND EXISTS (
       SELECT 1 FROM user_permissions liberado
        WHERE liberado.user_id     = user_permissions.user_id
          AND liberado.screen_code = 'communication/email-signature'
          AND liberado.permission  = 'INCLUIR'
          AND liberado.allowed
   );
