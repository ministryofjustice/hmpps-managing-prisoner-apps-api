CREATE TABLE IF NOT EXISTS establishment_application_type
(
    establishment_id VARCHAR(255) NOT NULL,
    application_group_id BIGINT NOT NULL,
    application_type_id BIGINT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6),
    last_modified_date TIMESTAMP(6),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),

    -- Primary key
    CONSTRAINT pk_est_app_type
        PRIMARY KEY (establishment_id, application_group_id, application_type_id),

    -- FK to establishment
    CONSTRAINT fk_est_app_type_establishment FOREIGN KEY (establishment_id) REFERENCES establishment (id),

    -- FK to application_type
    CONSTRAINT fk_est_app_type_app_type FOREIGN KEY (application_type_id) REFERENCES application_type (id),

    -- FK to establishment_application_group - CRITICAL
    -- This ensures a type can only exist if its parent group is configured for the establishment
    CONSTRAINT fk_est_app_type_est_app_group
        FOREIGN KEY (establishment_id, application_group_id)
            REFERENCES establishment_application_group (establishment_id, application_group_id)
);
CREATE INDEX idx_eat_establishment_active
    ON establishment_application_type(establishment_id, active);

CREATE INDEX idx_eat_establishment_group
    ON establishment_application_type(establishment_id, application_group_id);