-- O primeiro acesso deixa de ser exclusivo de funcionário: cliente também é
-- convidado por aqui. A FK sempre apontou para parceiros, a tabela que
-- Employee e Customer dividem - o destino não muda, só o nome deixa de mentir
ALTER TABLE first_access_token RENAME COLUMN employee_id to partner_id;

-- O e-mail do convite viaja no token. No fluxo do funcionário quem digita o
-- e-mail no fim é a própria pessoa; no do cliente isso não pode acontecer, ou
-- o convite troca de dono no meio do caminho.
ALTER TABLE first_access_token ADD COLUMN email VARCHAR(255);

-- A tela de acessos lista os convites pendentes de um parceiro toda vez que abre.
CREATE INDEX idx_first_access_token_partner ON first_access_token (partner_id);