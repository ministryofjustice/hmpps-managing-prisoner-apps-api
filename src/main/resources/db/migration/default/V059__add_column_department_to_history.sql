--Drop constraint
ALTER TABLE if exists app DROP CONSTRAINT if exists fk_app_assigned_group;
ALTER TABLE if exists app ADD COLUMN if not exists department_id UUID NULL;

ALTER TABLE IF EXISTS history ADD COLUMN if not exists department_name varchar(255) NULL;
