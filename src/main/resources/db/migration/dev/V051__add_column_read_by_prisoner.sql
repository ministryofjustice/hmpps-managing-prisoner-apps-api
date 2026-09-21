ALTER TABLE if exists comment
    ADD column if not exists read_by_prisoner boolean NOT NULL DEFAULT FALSE;