ALTER TABLE if exists comment DROP CONSTRAINT if exists fk_comment_app;
ALTER TABLE if exists response DROP CONSTRAINT if exists fk_response_app;
ALTER TABLE if exists history DROP CONSTRAINT if exists fk_history_app;

ALTER TABLE IF EXISTS comment
    ADD CONSTRAINT fk_comment_app
        FOREIGN KEY (app)
            REFERENCES app (id);

ALTER TABLE IF EXISTS response
    ADD CONSTRAINT fk_response_app
        FOREIGN KEY (app)
            REFERENCES app (id);

ALTER TABLE IF EXISTS history
    ADD CONSTRAINT fk_history_app
        FOREIGN KEY (app_id)
            REFERENCES app (id);

