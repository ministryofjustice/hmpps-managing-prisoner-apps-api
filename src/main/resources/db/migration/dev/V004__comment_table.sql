create table public.comment
(
    created_date timestamp(6),
    app          uuid,
    id           uuid not null
        primary key,
    message      varchar(1000),
    created_by   varchar(255)
);

alter table public.comment
    owner to postgres;

