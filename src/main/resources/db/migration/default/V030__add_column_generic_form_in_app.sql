ALTER TABLE if exists app
    ADD column if not exists generic_form boolean NOT NULL DEFAULT FALSE;
