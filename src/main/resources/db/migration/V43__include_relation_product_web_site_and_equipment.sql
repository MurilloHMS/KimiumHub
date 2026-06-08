ALTER TABLE products
ADD COLUMN concentracao VARCHAR(100),
ADD COLUMN local_de_uso VARCHAR(100);

CREATE TABLE equipment(
    id UUID PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    imagem VARCHAR(200) NOT NULL
);

CREATE TABLE product_equipment (
                                   product_id UUID NOT NULL,
                                   equipment_id UUID NOT NULL,

                                   CONSTRAINT pk_product_equipment
                                       PRIMARY KEY (product_id, equipment_id),

                                   CONSTRAINT fk_product_equipment_product
                                       FOREIGN KEY (product_id)
                                           REFERENCES products(id),

                                   CONSTRAINT fk_product_equipment_equipment
                                       FOREIGN KEY (equipment_id)
                                           REFERENCES equipment(id)
);

CREATE INDEX idx_product_equipment_product
    ON product_equipment(product_id);

CREATE INDEX idx_product_equipment_equipment
    ON product_equipment(equipment_id)