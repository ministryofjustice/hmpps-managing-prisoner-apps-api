CREATE TABLE IF NOT EXISTS establishment_application_group
(
    establishment_id VARCHAR(255) NOT NULL CONSTRAINT fk_est_app_group_establishment REFERENCES establishment,
    application_group_id BIGINT NOT NULL CONSTRAINT fk_est_app_group_app_group REFERENCES application_group,
    display_order INT  NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6),
    last_modified_date TIMESTAMP(6),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    CONSTRAINT pk_est_app_group PRIMARY KEY (establishment_id, application_group_id)
);

CREATE INDEX idx_eag_establishment_active
    ON establishment_application_group(establishment_id, active);

CREATE INDEX idx_eag_display_order
    ON establishment_application_group(establishment_id, display_order);
