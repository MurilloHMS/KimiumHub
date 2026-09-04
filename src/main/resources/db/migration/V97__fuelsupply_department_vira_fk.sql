-- O abastecimento passa a apontar para o cadastro de departamentos.
--
-- A coluna `department` guardava o nome do enum como texto. O funcionario
-- deixou de ter departamento proprio — quem decide e o SETOR dele, que
-- pertence a um departamento —, entao o abastecimento passa a apontar para a
-- mesma linha que o resto do sistema enxerga.
--
-- A coluna velha FICA. Enquanto ela existir da para voltar atras sem perder
-- nada, e a conferencia do backfill pode ser feita comparando as duas.

ALTER TABLE fuelsupply
    ADD COLUMN department_id UUID REFERENCES departments(id);

-- Casa por nome. Funciona porque a V54 semeou `departments` com exatamente os
-- valores do enum antigo.
UPDATE fuelsupply f
    SET department_id = d.id
    FROM departments d
    WHERE f.department = d.name;

-- O que nao casou — departamento renomeado depois da V54, ou lixo antigo —
-- vai para o balde que ja existe, em vez de travar o NOT NULL abaixo.
UPDATE fuelsupply
    SET department_id = (SELECT id FROM departments WHERE name = 'SEM_DEPARTAMENTO')
    WHERE department_id IS NULL;

ALTER TABLE fuelsupply
    ALTER COLUMN department_id SET NOT NULL;

-- A coluna antiga deixa de ser obrigatoria: o codigo nao escreve mais nela, e
-- insert novo falharia no NOT NULL dela.
ALTER TABLE fuelsupply
    ALTER COLUMN department DROP NOT NULL;
