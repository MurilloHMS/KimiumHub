-- Latitude e longitude no endereço, por causa do Uber.
--
-- Ele testou os quatro botões de "Como chegar" no iPhone em 2026-09-19: Waze,
-- Google Maps e Apple Maps abriram no endereço certo, e o Uber abriu pedindo
-- para digitar o destino. A causa não é o link — o formato foi corrigido no
-- site no mesmo dia e continuou abrindo vazio. É que o link universal da Uber
-- roteia por coordenadas; o endereço em texto do `drop[0]` serve só para
-- escrever na tela. Com latitude e longitude no mesmo link, abriu certo.
--
-- Quem preenche é o site, uma vez, na hora de salvar o endereço, pelo
-- Nominatim do OpenStreetMap (sem chave, como o mapa). Continuam opcionais:
-- endereço que o geocodificador não achou fica sem ponto, e aí o site mostra
-- só os três apps que sabem procurar por texto.
--
-- NUMERIC(9,6) guarda de -180,000000 a 180,000000 — seis casas são cerca de
-- 11 cm, mais do que o suficiente para um portão de empresa, e sem o arredonda-
-- mento traiçoeiro do ponto flutuante.
--
-- As mesmas duas colunas nas três tabelas que embutem o `Address`, como em V106.

ALTER TABLE companies
    ADD COLUMN address_latitude  NUMERIC(9, 6),
    ADD COLUMN address_longitude NUMERIC(9, 6);

ALTER TABLE company_events
    ADD COLUMN address_latitude  NUMERIC(9, 6),
    ADD COLUMN address_longitude NUMERIC(9, 6);

ALTER TABLE event_talks
    ADD COLUMN address_latitude  NUMERIC(9, 6),
    ADD COLUMN address_longitude NUMERIC(9, 6);

-- Ou as duas, ou nenhuma: meia coordenada não localiza nada, e deixar passar
-- uma só esconderia um erro de preenchimento até alguém abrir o Uber.
ALTER TABLE companies
    ADD CONSTRAINT companies_address_coords_check
        CHECK ((address_latitude IS NULL) = (address_longitude IS NULL));

ALTER TABLE company_events
    ADD CONSTRAINT company_events_address_coords_check
        CHECK ((address_latitude IS NULL) = (address_longitude IS NULL));

ALTER TABLE event_talks
    ADD CONSTRAINT event_talks_address_coords_check
        CHECK ((address_latitude IS NULL) = (address_longitude IS NULL));
