create table if not exists public.comment
(
    id           uuid not null
    primary key,
    created_date timestamp(6),
    app          uuid,
    message      varchar(1000),
    created_by   varchar(255)
    );

