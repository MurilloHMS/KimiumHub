CREATE TABLE rental_receipt_batches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference_month VARCHAR(20) NOT NULL,
    reference_year INT NOT NULL,
    generated_by TEXT REFERENCES users(id),
    generated_at TIMESTAMP NOT NULL,
    total_amount NUMERIC(15,2),
    source_filename VARCHAR(255)
);

CREATE TABLE rental_receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    batch_id UUID NOT NULL REFERENCES rental_receipt_batches(id),
    receipt_type VARCHAR(10) NOT NULL,
    cod_matriz VARCHAR(50) NOT NULL,
    nome_matriz VARCHAR(255) NOT NULL,
    num_nota VARCHAR(50),
    nome_parceiro VARCHAR(255),
    due_date DATE,
    total_amount NUMERIC(15,2) NOT NULL,
    total_unidades INT,
    total_maquinas INT,
    storage_path VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_rental_receipts_batch ON rental_receipts(batch_id);
CREATE INDEX idx_receipt_batches_ref ON rental_receipt_batches(reference_month, reference_year);
