create table if not exists public.history
(
    id            uuid not null
    primary key,
    created_date  timestamp(6),
    app_id        uuid,
    entity_id     uuid,
    activity      varchar(255)
    constraint history_activity_check
    check ((activity)::text = ANY
((ARRAY ['APP_SUBMITTED'::character varying, 'APP_REQUEST_FORM_DATA_UPDATED'::character varying, 'APP_FORWARDED_TO_A_GROUP'::character varying, 'COMMENT_ADDED'::character varying, 'FORWARDING_COMMENT_ADDED'::character varying, 'RESPONSE_ADDED'::character varying, 'APP_APPROVED'::character varying, 'APP_DECLINED'::character varying])::text[])),
    created_by    varchar(255),
    entity_type   varchar(255)
    constraint history_entity_type_check
    check ((entity_type)::text = ANY
((ARRAY ['APP'::character varying, 'COMMENT'::character varying, 'RESPONSE'::character varying, 'ASSIGNED_GROUP'::character varying])::text[])),
    establishment varchar(255)
    );
