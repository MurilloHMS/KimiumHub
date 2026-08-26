-- O estoque atual era sorteado entre os movimentos do dia.
--
-- `currentStock` lê o último movimento ordenando por `movement_date DESC,
-- id DESC`. Só que `movement_date` é `date`, sem hora: dois movimentos do mesmo
-- dia empatam, e o desempate cai no id — um UUID aleatório. Na prática, qual
-- linha ganhava era sorteio.
--
-- A V20 se chama "update_movement_date_to_local_date_time" e só trocou o
-- DEFAULT da coluna; o tipo nunca mudou. A entidade declara `LocalDateTime`
-- desde então, então o Hibernate manda a hora e o Postgres descarta — sem erro,
-- sem aviso.
--
-- Ficou invisível por anos porque dois lançamentos do mesmo produto no mesmo
-- dia eram raros. A conciliação de máquinas fez cada mudança de status virar um
-- lançamento, e aí o defeito passou a aparecer todo dia.
--
-- Trocar o tipo de `movement_date` não bastaria: o desktop continua no ar e
-- manda só a data. Um lançamento dele às 15h viraria meia-noite e ficaria atrás
-- de um da API feito às 14h. Uma coluna preenchida pelo BANCO vale para os dois
-- clientes sem tocar em nenhum.

-- `clock_timestamp()` e não `now()`: no Postgres o `now()` é o instante de
-- início da TRANSAÇÃO e fica constante nela inteira. Dois lançamentos na mesma
-- transação receberiam o mesmo valor e empatariam de novo — que é o defeito
-- que esta coluna existe para resolver.
ALTER TABLE products_movements
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT clock_timestamp();

-- O histórico precisa de uma ordem, e nivelar tudo em meia-noite só repetiria o
-- empate: o estoque dos produtos que já têm dois lançamentos no mesmo dia
-- continuaria sorteado até alguém lançar de novo.
--
-- `ctid` é o endereço físico da linha. Esta tabela **só recebe insert** — nunca
-- update, nunca delete pela aplicação — então a ordem física é, na prática, a
-- ordem de inserção. Não é garantia do Postgres (um VACUUM FULL ou um CLUSTER
-- reorganizariam), mas é a única pista que restou da ordem real, e é muito
-- melhor que sorteio.
--
-- Um segundo de distância entre linhas do mesmo dia basta para desempatar, e
-- mantém todo o histórico atrás de qualquer lançamento novo.
UPDATE products_movements m
SET created_at = m.movement_date::timestamptz + (ordem.posicao * interval '1 second')
FROM (
    SELECT ctid,
           row_number() OVER (
               PARTITION BY product_id, movement_date
               ORDER BY ctid
           ) AS posicao
    FROM products_movements
) ordem
WHERE m.ctid = ordem.ctid;

-- Ler o último movimento por produto é a consulta mais quente do estoque, e ela
-- passa a ordenar por esta coluna.
CREATE INDEX ix_products_movements_product_created
    ON products_movements (product_id, created_at DESC, id DESC);

COMMENT ON COLUMN products_movements.movement_date IS
    'Quando a movimentação aconteceu. Informado por quem lançou, e editável.';
COMMENT ON COLUMN products_movements.created_at IS
    'Quando foi registrada. Do sistema, imutável, e é por ela que o estoque atual é lido.';
