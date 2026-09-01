ALTER TABLE if exists app DROP CONSTRAINT if exists fk_app_assigned_group;
ALTER TABLE if exists app DROP CONSTRAINT if exists fk_app_application_group;
ALTER TABLE if exists app DROP CONSTRAINT if exists fk_app_application_type;
ALTER TABLE if exists app DROP CONSTRAINT if exists fk_app_establishment;

ALTER TABLE IF EXISTS app
    ADD CONSTRAINT fk_app_assigned_group
        FOREIGN KEY (assigned_group)
            REFERENCES groups (id);

ALTER TABLE IF EXISTS app
    ADD CONSTRAINT fk_app_application_group
        FOREIGN KEY (application_group)
            REFERENCES application_group (id);

ALTER TABLE IF EXISTS app
    ADD CONSTRAINT fk_app_application_type
        FOREIGN KEY (application_type)
            REFERENCES application_type (id);

ALTER TABLE IF EXISTS app
    ADD CONSTRAINT fk_app_establishment
        FOREIGN KEY (establishment_id)
            REFERENCES establishment (id);