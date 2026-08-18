-- Máquina deixa de ser um TIPO de produto e passa a ser um produto MARCADO.
--
-- O discriminador `type` é exclusivo: uma linha é INVENTORY ou MACHINE, nunca
-- as duas. Como todo repositório tipado numa subclasse ganha `WHERE type='X'`
-- do Hibernate, a máquina era invisível para o módulo de estoque — e por isso
-- não dava para controlar o estoque dela pela tela de movimentações.
--
-- Uma flag compõe onde o discriminador exclui: a linha passa a ser um produto
-- de estoque que TAMBÉM é máquina, com um código só e um histórico só.
ALTER TABLE products ADD COLUMN is_machine BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE products SET is_machine = TRUE, type = 'INVENTORY' WHERE type = 'MACHINE';

-- Movimento de máquina vira movimento de produto: uma tabela só, a mesma tela.
-- Duas tabelas idênticas seriam a herança de volta, disfarçada — e manteriam a
-- máquina fora do estoque baixo, do hub e do relatório.
--
-- As duas colunas de data são DATE: a V20 se chama "update to local date time"
-- mas só trocou o DEFAULT, nunca o tipo. Cópia direta, sem cast.
INSERT INTO products_movements (id, movement_date, quantity, product_id)
SELECT id, movement_date, quantity, machine_id FROM machine_movements;

DROP TABLE machine_movements;
