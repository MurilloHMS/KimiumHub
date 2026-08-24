-- Um código de produto passa a ser único por tipo.
--
-- `products.system_code` nunca teve unique — a V8 criou a coluna como um
-- VARCHAR(9) NOT NULL e mais nada. A planilha de produtos inseria sem olhar o
-- que já tinha entrado na própria lista, então código repetido no mesmo arquivo
-- virava duas linhas. `findBySystemCode` devolve uma entidade só, e a partir da
-- segunda linha ele estoura NonUniqueResultException — 500 na tela de
-- movimentações. Aconteceu em 2026-08-11 e de novo em 2026-08-24.
--
-- O índice é (system_code, type), não só system_code: `products` é herança de
-- tabela única, e o mesmo código existe de propósito uma vez como WEBSITE e
-- uma vez como INVENTORY.
--
-- A limpeza vem ANTES do índice, no mesmo arquivo. Com duplicado na base o
-- CREATE UNIQUE INDEX falha, o Flyway aborta e a API não sobe — a tela quebrada
-- viraria indisponibilidade total.

-- 1. Elege o sobrevivente de cada grupo: o que carrega mais histórico. O
--    desempate por id existe para o resultado ser sempre o mesmo, rodando onde
--    rodar.
CREATE TEMP TABLE produto_duplicado AS
WITH classificado AS (
    SELECT p.id,
           p.system_code,
           p.type,
           row_number() OVER (
               PARTITION BY p.system_code, p.type
               ORDER BY (SELECT count(*) FROM products_movements m WHERE m.product_id = p.id) DESC,
                        (SELECT count(*) FROM machine_registers  r WHERE r.machine_id = p.id) DESC,
                        p.id
           ) AS posicao
    FROM products p
),
sobrevivente AS (
    SELECT system_code, type, id FROM classificado WHERE posicao = 1
)
SELECT c.id AS perdedor, s.id AS sobrevivente
FROM classificado c
JOIN sobrevivente s
  ON s.system_code = c.system_code
 AND s.type = c.type
WHERE c.posicao > 1;

-- 2. Traz o histórico para o sobrevivente ANTES de apagar qualquer coisa.
--    products_movements e machine_registers têm ON DELETE CASCADE: apagar o
--    produto primeiro levaria movimento e programação junto, sem erro e sem
--    log.
UPDATE products_movements m
SET product_id = d.sobrevivente
FROM produto_duplicado d
WHERE m.product_id = d.perdedor;

UPDATE machine_registers r
SET machine_id = d.sobrevivente
FROM produto_duplicado d
WHERE r.machine_id = d.perdedor;

-- product_equipment tem chave primária composta (product_id, equipment_id).
-- Mover cego criaria linha duplicada, então migra só o vínculo que falta no
-- sobrevivente e descarta o resto.
UPDATE product_equipment pe
SET product_id = d.sobrevivente
FROM produto_duplicado d
WHERE pe.product_id = d.perdedor
  AND NOT EXISTS (
      SELECT 1 FROM product_equipment ja
      WHERE ja.product_id = d.sobrevivente
        AND ja.equipment_id = pe.equipment_id
  );

DELETE FROM product_equipment pe
USING produto_duplicado d
WHERE pe.product_id = d.perdedor;

-- 3. A marca de máquina não pode se perder no caminho: se qualquer perdedor
--    era máquina, o sobrevivente passa a ser. Sem isto a máquina sumiria da
--    programação e do filtro do estoque.
UPDATE products p
SET is_machine = TRUE
FROM produto_duplicado d
WHERE p.id = d.sobrevivente
  AND EXISTS (
      SELECT 1 FROM products perdedor
      WHERE perdedor.id = d.perdedor
        AND perdedor.is_machine
  );

-- 4. Agora os perdedores estão vazios e podem sair.
DELETE FROM products p
USING produto_duplicado d
WHERE p.id = d.perdedor;

DROP TABLE produto_duplicado;

-- 5. O índice que impede a volta.
CREATE UNIQUE INDEX ux_products_system_code_type
    ON products (system_code, type);
