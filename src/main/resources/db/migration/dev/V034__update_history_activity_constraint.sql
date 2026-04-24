ALTER TABLE if exists history DROP CONSTRAINT if exists history_activity_check;

ALTER TABLE if exists history ADD CONSTRAINT history_activity_check
    check ((activity)::text = ANY
    ((ARRAY
    [
    'APP_SUBMITTED':: character varying,
    'APP_REQUEST_FORM_DATA_UPDATED':: character varying,
    'APP_FORWARDED_TO_A_GROUP':: character varying,
    'COMMENT_ADDED':: character varying,
    'FORWARDING_COMMENT_ADDED':: character varying,
    'RESPONSE_ADDED':: character varying,
    'APP_APPROVED':: character varying,
    'APP_DECLINED':: character varying,
    'FILE_ADDED':: character varying,
    'PRISONER_ID_UPDATE'::character varying
    ]
    )::text[]));