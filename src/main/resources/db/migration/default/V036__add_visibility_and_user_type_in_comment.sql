ALTER TABLE if exists comment
    ADD column if not exists visibility varchar(255);

ALTER TABLE if exists comment
    ADD column if not exists created_by_user_type varchar(255);
