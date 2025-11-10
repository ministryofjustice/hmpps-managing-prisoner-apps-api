ALTER TABLE if exists app
    ADD column if not exists application_group bigint;

ALTER TABLE if exists app
    ADD column if not exists application_type bigint;
