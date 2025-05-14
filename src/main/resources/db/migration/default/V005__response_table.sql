create table if not exists public.response
(
    created_date timestamp(6),
    id           uuid not null
    primary key,
    reason       varchar(1000),
    created_by   varchar(255),
    decision     varchar(255)
    constraint response_decision_check
    check ((decision)::text = ANY
((ARRAY ['APPROVED'::character varying, 'DECLINED'::character varying])::text[]))
    );
