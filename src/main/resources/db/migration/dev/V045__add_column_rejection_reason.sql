ALTER TABLE if exists response
    ADD column if not exists rejection_reason varchar(256);

ALTER TABLE if exists response DROP CONSTRAINT if exists response_decision_check;
ALTER TABLE if exists app DROP CONSTRAINT if exists app_status_check;
ALTER TABLE if exists history DROP CONSTRAINT if exists history_activity_check;

ALTER TABLE if exists response
    ADD CONSTRAINT response_decision_check
    CHECK ((decision)::text = ANY
    ((ARRAY ['APPROVED'::character varying, 'DECLINED'::character varying, 'REJECTED'::character varying])::text[]));

ALTER TABLE if exists app
    ADD CONSTRAINT app_status_check
    CHECK ((status)::text = ANY
    ((ARRAY ['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'DECLINED'::character varying])::text[]));

ALTER TABLE if exists history ADD CONSTRAINT history_activity_check
    CHECK ((activity)::text = ANY
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
               'APP_REJECTED':: character varying,
               'PRISONER_ID_UPDATE'::character varying
               ]
               )::text[]));