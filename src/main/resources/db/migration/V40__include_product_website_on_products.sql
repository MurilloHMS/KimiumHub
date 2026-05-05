alter table products
add column imagem VARCHAR(200),
add column cores JSONB,
add column finalidade VARCHAR(500),
add column diluicao VARCHAR(100),
add column descricao VARCHAR(1000);

CREATE INDEX idx_product_cores ON products USING GIN(cores);