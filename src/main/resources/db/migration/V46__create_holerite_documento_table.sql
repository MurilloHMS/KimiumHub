create table holerite_documento (
    id uuid primary key,
    employee_id uuid not null references parceiros(id) on delete cascade,
    competencia date not null,
    original_filename varchar(255),
    storage_path varchar(500) not null,
    created_at timestamp not null default now()
);

create index idx_holerite_employee on holerite_documento(employee_id);
create index idx_holerite_competencia on holerite_documento(competencia);
