-- Um usuário do portal pertence a um cliente. Sem unique, ao contrário de
-- employee_id: uma unidade tem várias pessoas com acesso, e o e-mail do
-- cadastro se repete em 86 grupos hoje.
ALTER TABLE users ADD COLUMN customer_id UUID REFERENCES parceiros(id);

-- Matriz é decisão de cadastro, não convenção implícita: hoje "matriz" seria
-- adivinhado por codigo_matriz = cod_parceiro, e isso não está escrito em
-- lugar nenhum do código.
ALTER TABLE parceiros ADD COLUMN is_matriz BOOLEAN NOT NULL DEFAULT FALSE;

-- Login por CNPJ precisa achar um cliente só. O índice é sobre os dígitos
-- porque documento é gravado como o usuário digitou, com ou sem pontuação.
CREATE UNIQUE INDEX ux_parceiros_cnpj_digits
    ON parceiros (regexp_replace(documento, '[^0-9]', '', 'g'))
    WHERE perfil = 'CLIENTE' AND documento IS NOT NULL AND documento <> '';