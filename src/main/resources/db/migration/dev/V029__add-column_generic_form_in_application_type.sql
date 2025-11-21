ALTER TABLE if exists application_type
    ADD column if not exists generic_form boolean NOT NULL DEFAULT FALSE;
