ALTER TABLE if exists comment DROP CONSTRAINT if exists comment_created_by_user_type_check;

ALTER TABLE if exists comment ADD CONSTRAINT comment_created_by_user_type_check
    check ((created_by_user_type)::text = ANY
    ((ARRAY ['PRISONER'::character varying, 'STAFF'::character varying])::text[]));

ALTER TABLE if exists comment DROP CONSTRAINT if exists comment_visibility_check;

ALTER TABLE if exists comment ADD CONSTRAINT comment_visibility_check
    check ((visibility)::text = ANY
    ((ARRAY ['STAFF_AND_PRISONER'::character varying, 'STAFF_ONLY'::character varying])::text[]));
