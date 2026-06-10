create table profile (
     id uuid primary key,
     nome varchar(150) not null,
     slug varchar(150) not null unique,
     cargo varchar(150),
     empresa varchar(150),
     email varchar(200) not null,
     imagem varchar(255),
     descricao varchar(1000),
     telefones jsonb not null,
     redes_sociais jsonb not null,
     regioes_atendimento jsonb not null,
     segmentos_atendimento jsonb not null,
     ativo boolean not null default true,
     created_at timestamp not null default now(),
     updated_at timestamp not null default now()
);

create index idx_profile_slug on profile(slug);
create index idx_profile_ativo on profile(ativo);