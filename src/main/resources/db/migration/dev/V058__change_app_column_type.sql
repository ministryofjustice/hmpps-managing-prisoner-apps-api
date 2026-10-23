ALTER TABLE IF EXISTS app DROP COLUMN if exists department_id;
ALTER TABLE IF EXISTS app ADD COLUMN if not exists department_slug varchar(255) NULL;
