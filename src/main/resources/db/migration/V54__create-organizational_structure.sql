CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    legal_name VARCHAR(150) NOT NULL,
    cnpj VARCHAR(18) NOT NULL UNIQUE
);

CREATE TABLE departments(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    department_id UUID NOT NULL REFERENCES departments(id)
);

CREATE TABLE hierarchies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    level_order INT NOT NULL
);

ALTER TABLE parceiros
    ADD COLUMN company_id UUID REFERENCES companies(id),
    ADD COLUMN department_id UUID REFERENCES departments(id),
    ADD COLUMN team_id UUID REFERENCES teams(id),
    ADD COLUMN hierarchy_id UUID REFERENCES hierarchies(id);

-- Seed
INSERT INTO departments (name) VALUES
    ('RESTAURANTES'), ('AUTOMOTIVO'), ('ALIMENTOS'), ('SUL'), ('EQUIPAMENTOS'),
    ('LAVANDERIA'), ('MOTORISTA'), ('MANUTENCAO'), ('DISTRIBUIDORES'),
    ('ADMINISTRATIVO'), ('PRODUCAO'), ('SEM_DEPARTAMENTO');


INSERT INTO hierarchies (name, level_order) VALUES
    ('DIRETOR', 1), ('CEO', 2), ('SUPERVISOR', 3), ('GERENTE', 4),
    ('COORDENADOR', 5), ('ANALISTA', 6), ('ASSISTENTE', 7);

-- Backfill
UPDATE parceiros p SET department_id = d.id
    FROM departments d WHERE p.departamento = d.name;

UPDATE parceiros p SET hierarchy_id = h.id
    FROM hierarchies h WHERE p.hierarquia = h.name;