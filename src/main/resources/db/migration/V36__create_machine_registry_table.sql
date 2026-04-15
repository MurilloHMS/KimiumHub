create table machine_registers(
    id UUID PRIMARY KEY,
    machine_id UUID NOT NULL,
    nome_cliente VARCHAR(200) NOT NULL,
    tag smallint,
    solicitante VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    observacao VARCHAR(500),
    previsao_entrega TIMESTAMP,
    tecnico VARCHAR(100),

    FOREIGN KEY (machine_id) REFERENCES products(id) ON DELETE CASCADE
)