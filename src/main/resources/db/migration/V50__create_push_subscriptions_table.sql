-- Inscrições de Web Push (VAPID) por dispositivo/navegador do usuário.
create table push_subscriptions (
    id uuid primary key,
    recipient_login varchar(255) not null,
    endpoint varchar(500) not null unique,
    p256dh varchar(255) not null,
    auth varchar(255) not null,
    created_at timestamp not null default now()
);

create index idx_push_recipient on push_subscriptions(recipient_login);
