ALTER TABLE if exists app
ADD column if not exists first_night_center boolean NOT NULL DEFAULT FALSE;
