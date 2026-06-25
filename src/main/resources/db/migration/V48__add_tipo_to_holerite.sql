-- Cada mês tem dois holerites: ADIANTAMENTO (dia 20) e SALARIO/folha (dia 05).
-- O RH informa o tipo ao vincular, e o funcionário vê os dois separadamente.
alter table holerite_documento
    add column tipo varchar(20) not null default 'SALARIO';

create index idx_holerite_tipo on holerite_documento(tipo);
