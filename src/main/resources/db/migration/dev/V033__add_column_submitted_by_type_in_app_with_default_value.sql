ALTER TABLE if exists app
    ADD column if not exists submitted_by_type varchar (255)
    constraint app_submitted_by_type_check
    check ((submitted_by_type)::text = ANY
    ((ARRAY ['PRISONER':: character varying, 'STAFF':: character varying])::text[]));

update app
set submitted_by_type = 'STAFF' where created_by != requested_by;
