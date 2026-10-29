ALTER TABLE IF EXISTS establishment_application_type DROP COLUMN IF EXISTS department_id;
ALTER TABLE IF EXISTS establishment_application_type ADD COLUMN IF NOT EXISTS department_slug VARCHAR(255) NULL;
