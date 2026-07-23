ALTER TABLE if exists response
    ADD column if not exists rejection_reason varchar(256);

ALTER TABLE if exists response DROP CONSTRAINT if exists response_decision_check;

ALTER TABLE if exists response
    ADD CONSTRAINT response_decision_check
    CHECK ((decision)::text = ANY
((ARRAY ['APPROVED'::character varying, 'DECLINED'::character varying, 'REJECTED'::character varying])::text[]));
