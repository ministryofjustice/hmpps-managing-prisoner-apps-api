ALTER TABLE if exists establishment
ADD column if not exists default_departments boolean NOT NULL DEFAULT FALSE;
