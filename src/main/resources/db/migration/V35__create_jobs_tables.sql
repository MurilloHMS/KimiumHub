CREATE TABLE vagas (
    id UUID PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    requisitos VARCHAR(1000) NOT NULL,
    beneficios VARCHAR(1000) NOT NULL,
    area VARCHAR(100),
    status VARCHAR(9) NOT NULL,
    data_abertura TIMESTAMP,
    data_encerramento TIMESTAMP
)

CREATE TABLE candidatos (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telefone VARCHAR(11) NOT NULL,
    url_linkedin VARCHAR(100),
    path_curriculo VARCHAR(200),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

CREATE TABLE candidaturas (
    id UUID PRIMARY KEY,
    candidato_id UUID NOT NULL,
    vaga_id UUID NOT NULL,
    etapa_atual VARCHAR(20),
    status VARCHAR(20),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT,

    FOREIGN KEY (candidato_id) REFERENCES candidatos(id),
    FOREIGN KEY (vaga_id) REFERENCES vagas(id)
)

CREATE TABLE historico_etapas (
    id UUID PRIMARY KEY,
    candidatura_id UUID NOT NULL,
    etapa_anterior VARCHAR(20),
    etapa_nova VARCHAR(20),
    status VARCHAR(20),
    observacao VARCHAR(200),
    data_movimentacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (candidatura_id) REFERENCES candidaturas(id)
)

CREATE TABLE perguntas_personalizadas (
    id UUID PRIMARY KEY,
    vaga_id UUID NOT NULL,
    enunciado VARCHAR(1000) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    obrigatoria BOOLEAN DEFAULT FALSE,
    ordem SMALLINT NOT NULL,

    FOREIGN KEY (vaga_id) REFERENCES vagas(id)
)

CREATE TABLE resposta_perguntas(
    id UUID PRIMARY KEY,
    candidatura_id UUID NOT NULL,
    pergunta_id UUID NOT NULL,
    resposta VARCHAR(500) NOT NULL,
    respondido_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (candidatura_id) REFERENCES candidaturas(id),
    FOREIGN KEY (pergunta_id) REFERENCES perguntas_personalizadas(id)
)

CREATE TABLE template_email (
    id UUID PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    etapa VARCHAR(20) NOT NULL,
    assunto VARCHAR(50) NOT NULL,
    corpo VARCHAR(1000) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE
)

CREATE TABLE notificacao_processo_seletivo (
    id UUID PRIMARY KEY,
    candidatura_id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    assunto VARCHAR(50) NOT NULL,
    corpo VARCHAR(1000) NOT NULL,
    enviado BOOLEAN DEFAULT FALSE,
    enviado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)