ALTER TABLE if exists establishment_application_type
    DROP CONSTRAINT IF EXISTS fk_est_app_type_est_app_group;

DROP INDEX IF EXISTS idx_eat_establishment_group;

-- Introduce a UUID surrogate primary key
ALTER TABLE if exists establishment_application_type
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();

-- Replace the old composite primary key with the new UUID id
ALTER TABLE if exists establishment_application_type
    DROP CONSTRAINT IF EXISTS pk_est_app_type;

ALTER TABLE if exists establishment_application_type
    ADD CONSTRAINT pk_est_app_type PRIMARY KEY (id);

-- Preserve uniqueness of establishment + application type now the composite key is gone
ALTER TABLE if exists establishment_application_type
    ADD CONSTRAINT uq_est_app_type_establishment_type UNIQUE (establishment_id, application_type_id);

-- foreign key references. establishment_application_type must reference
-- establishment, application_type.
ALTER TABLE if exists establishment_application_type
    DROP CONSTRAINT IF EXISTS fk_est_app_type_establishment;

ALTER TABLE if exists establishment_application_type
    ADD CONSTRAINT fk_est_app_type_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishment (id);

ALTER TABLE if exists establishment_application_type
    DROP CONSTRAINT IF EXISTS fk_est_app_type_app_type;

ALTER TABLE if exists establishment_application_type
    ADD CONSTRAINT fk_est_app_type_app_type
        FOREIGN KEY (application_type_id) REFERENCES application_type (id);

ALTER TABLE if exists establishment_application_type DROP COLUMN IF EXISTS application_group_id;
ALTER TABLE if exists establishment_application_type DROP COLUMN IF EXISTS display_order;

-- establishment_application_group is no longer used
DROP TABLE IF EXISTS establishment_application_group;
