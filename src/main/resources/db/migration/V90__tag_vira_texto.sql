-- A tag da programação vira texto.
--
-- Era `smallint`, e isso limitava a etiqueta a número — o time usa código com
-- letra. Dez caracteres é o teto pedido.
--
-- A conversão é de tipo E de valor. `0` sempre significou "sem tag" na prática:
-- a tela já mostrava traço para ele (`row.tag || '—'`), e a entidade usava
-- `short` primitivo, que não aceita nulo — então "vazio" só podia ser escrito
-- como zero. Agora que a coluna aceita nulo, o banco passa a dizer o que a tela
-- sempre disse.
--
-- `NULLIF` faz as duas coisas de uma vez, e a ordem importa: o `::varchar`
-- acontece antes, então a comparação é com a string '0' e não com o número.
-- Linha que já era nula continua nula — `NULL::varchar` é `NULL`, e
-- `NULLIF(NULL, '0')` também.
--
-- Não há risco de estouro: `smallint` vai até 32767, cinco dígitos mais o
-- sinal, bem abaixo dos dez.
ALTER TABLE machine_registers
    ALTER COLUMN tag TYPE VARCHAR(10) USING NULLIF(tag::varchar, '0');
