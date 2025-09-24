CREATE TABLE parceiros(
    id UUID PRIMARY KEY,
    cod_parceiro VARCHAR(9) NOT NULL,
    documento VARCHAR(14),
    email VARCHAR(200) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    recebe_email BOOLEAN,
    codigo_matriz VARCHAR(9),
    codigo_gerente VARCHAR(9),
    hierarquia VARCHAR(15),
    departamento VARCHAR(50),
    perfil VARCHAR(20) NOT NULL
)