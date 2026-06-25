-- Notificações persistidas (fonte da verdade); o tempo real (STOMP) e o web push
-- são apenas o "empurrão" ao vivo. A tela de notificações lê desta tabela.
create table notifications (
    id uuid primary key,
    recipient_login varchar(255) not null,
    type varchar(40) not null,
    title varchar(200) not null,
    message varchar(500) not null,
    link varchar(300),
    is_read boolean not null default false,
    created_at timestamp not null default now()
);

create index idx_notifications_recipient on notifications(recipient_login);
create index idx_notifications_unread on notifications(recipient_login, is_read);
