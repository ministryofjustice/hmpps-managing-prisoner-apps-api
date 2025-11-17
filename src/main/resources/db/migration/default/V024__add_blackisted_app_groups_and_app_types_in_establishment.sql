ALTER TABLE if exists establishment
    ADD column if not exists black_listed_app_groups jsonb;

ALTER TABLE if exists establishment
    ADD column if not exists black_listed_app_types  jsonb;

