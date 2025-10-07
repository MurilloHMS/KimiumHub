CREATE TABLE newsletter (
	id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
	codigo_cliente VARCHAR(50) NOT NULL,
	nome_do_cliente VARCHAR(255) NOT NULL,
	data DATE NOT NULL,
	mes VARCHAR(20) NOT NULL,
	quantidade_de_produtos INT NOT NULL,
	quantidade_de_litros DOUBLE PRECISION NOT NULL,
	quantidade_de_visitas INT NOT NULL,
	quantidade_notas_emitidas INT NOT NULL,
	media_dias_atendimento INT,
	produto_em_destaque VARCHAR(255),
	faturamento_total DOUBLE PRECISION,
	valor_de_pecas_trocadas DOUBLE PRECISION,
	status VARCHAR(50),
	email_cliente VARCHAR(255)
)