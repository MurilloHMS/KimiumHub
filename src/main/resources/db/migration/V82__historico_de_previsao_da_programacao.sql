-- Toda vez que uma previsão de saída é adiada, fica registrado o porquê.
--
-- É tabela e não coluna de propósito. Com um `motivo_alteracao` em
-- machine_registers, o segundo adiamento apagaria o primeiro — e some
-- justamente a informação que interessa, que é a máquina viver atrasando.
-- Uma linha por alteração responde "essa já foi adiada quantas vezes?".
--
-- Precedente de tabela de histórico no projeto: career_histories (V56).
CREATE TABLE machine_register_schedule_changes(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    register_id UUID NOT NULL REFERENCES machine_registers(id) ON DELETE CASCADE,

    -- Nunca nulo: só existe linha aqui quando já havia data. Preencher a
    -- previsão pela primeira vez é completar cadastro, não adiar.
    previsao_anterior TIMESTAMP NOT NULL,

    -- Nulo quando alguém apaga a previsão em vez de trocar por outra.
    previsao_nova TIMESTAMP,

    motivo VARCHAR(500) NOT NULL,
    changed_by VARCHAR(120),
    changed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_schedule_changes_register
    ON machine_register_schedule_changes (register_id, changed_at DESC);
