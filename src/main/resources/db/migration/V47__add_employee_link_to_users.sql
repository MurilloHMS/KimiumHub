-- Vínculo explícito entre usuário (auth) e funcionário (parceiro).
-- Antes o vínculo era apenas convencional: users.login == parceiros.username.
-- Agora há uma FK real, com integridade referencial e unicidade (1 usuário <-> 1 funcionário).

alter table users
    add column employee_id uuid;

alter table users
    add constraint fk_users_employee
        foreign key (employee_id) references parceiros (id) on delete set null;

alter table users
    add constraint uq_users_employee unique (employee_id);

-- Backfill: aproveita a convenção atual (username == login) para popular a FK
-- sem perder nenhum vínculo já existente.
update users u
set employee_id = p.id
from parceiros p
where p.perfil = 'FUNCIONARIO'
  and p.username is not null
  and p.username <> ''
  and p.username = u.login
  and u.employee_id is null;
