-- O upload de arquivo único faz saveAll puro: reenviar a planilha duplicava o
-- mês inteiro. Com a dashboard lendo série histórica, duplicata vira gráfico
-- errado, não linha repetida numa tabela que ninguém olha.
CREATE UNIQUE INDEX ux_newsletter_cliente_data ON newsletter (codigo_cliente, data);