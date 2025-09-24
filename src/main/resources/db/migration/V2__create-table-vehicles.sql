CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE vehicles(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(255) NOT NULL,
    placa VARCHAR(8) NOT NULL,
    marca VARCHAR(100) NOT NULL,
    consumo_urbano_alcool DOUBLE PRECISION NULL,
    consumo_urbano_gasolina DOUBLE PRECISION NULL,
    consumo_rodoviario_alcool DOUBLE PRECISION NULL,
    consumo_rodoviario_gasolina DOUBLE PRECISION NULL
);