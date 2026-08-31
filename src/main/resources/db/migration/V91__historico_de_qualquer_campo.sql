-- O histórico deixa de ser só de previsão.
--
-- A V82 criou esta tabela para uma coisa só: adiamento de data. Por isso as
-- colunas têm nome de data e tipo de data. Oito campos editáveis da programação
-- não têm rastro nenhum, e `previsao_anterior TIMESTAMP` não serve para guardar
-- "Cliente A" virando "Cliente B".
--
-- As colunas viram genéricas e `campo` diz o que mudou. Uma tabela e não oito:
-- o que muda entre um campo e outro é o VALOR, não a ESTRUTURA — e a tela lê
-- todas as alterações de uma linha do mesmo jeito, ordenadas por data,
-- independentemente de qual campo foi.
ALTER TABLE machine_register_schedule_changes
    ADD COLUMN campo          VARCHAR(40),
    ADD COLUMN valor_anterior VARCHAR(500),
    ADD COLUMN valor_novo     VARCHAR(500);

-- As linhas que já existem são todas adiamento de previsão, por construção.
--
-- `to_char` e não `::varchar`, e a diferença NÃO é cosmética. O cast do
-- Postgres produz `2026-08-31 14:30:00`, com espaço; `LocalDateTime.parse`
-- exige o `T` do ISO-8601 e estoura com espaço. Como o Hub relê estes valores
-- e os converte de volta para data, o cast direto deixaria todo o histórico
-- anterior ao deploy ilegível — e o erro só apareceria no Hub, não aqui.
--
-- Os segundos entram sempre. `LocalDateTime.toString()` os omite quando são
-- zero, mas o `parse` aceita as duas formas, então gravar a mais completa é o
-- lado seguro.
UPDATE machine_register_schedule_changes
   SET campo = 'previsao',
       valor_anterior = to_char(previsao_anterior, 'YYYY-MM-DD"T"HH24:MI:SS'),
       valor_novo     = to_char(previsao_nova,     'YYYY-MM-DD"T"HH24:MI:SS');

ALTER TABLE machine_register_schedule_changes
    -- Toda linha sabe de que campo é. Sem isso o Hub não consegue separar
    -- adiamento de troca de técnico, e "adiamentos no mês" passa a contar
    -- qualquer edição.
    ALTER COLUMN campo SET NOT NULL,

    -- O motivo vira opcional, por decisão do time. Continua sendo perguntado
    -- quando a previsão muda — perguntar sem obrigar é o que mantém alguém
    -- preenchendo; obrigar ensina a digitar "ok" para passar da tela.
    ALTER COLUMN motivo DROP NOT NULL,

    DROP COLUMN previsao_anterior,
    DROP COLUMN previsao_nova;
