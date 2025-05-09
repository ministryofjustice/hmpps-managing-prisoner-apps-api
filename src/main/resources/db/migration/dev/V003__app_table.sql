create table public.app
(
    id                      uuid not null
        primary key,
    app_type                varchar(255)
        constraint app_app_type_check
            check ((app_type)::text = ANY
        ((ARRAY ['PIN_PHONE_CREDIT_TOP_UP'::character varying, 'PIN_PHONE_EMERGENCY_CREDIT_TOP_UP'::character varying, 'PIN_PHONE_ADD_NEW_CONTACT'::character varying, 'PIN_PHONE_REMOVE_CONTACT'::character varying, 'PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS'::character varying, 'PIN_PHONE_SUPPLY_LIST_OF_CONTACTS'::character varying])::text[])),
    created_date            timestamp(6),
    last_modified_date      timestamp(6),
    requested_date          timestamp(6),
    assigned_group          uuid,
    created_by              varchar(255),
    establishment_id        varchar(255),
    last_modified_by        varchar(255),
    reference               varchar(255),
    requested_by            varchar(255),
    requested_by_first_name varchar(255),
    requested_by_last_name  varchar(255),
    status                  varchar(255)
        constraint app_status_check
            check ((status)::text = ANY
                   ((ARRAY ['PENDING'::character varying, 'APPROVED'::character varying, 'DECLINED'::character varying])::text[])),
    requests                jsonb
);

create table if not exists public.app_comments
(
    app_id   uuid not null
    constraint fkc1sltfa9f2a4p5s5wnso0awaw
    references public.app,
    comments uuid
);

create table if not exists public.app_responses
(
    app_id    uuid not null
    constraint fkon3hn0r56m2i3mta94q1agdqh
    references public.app,
    responses uuid
);

