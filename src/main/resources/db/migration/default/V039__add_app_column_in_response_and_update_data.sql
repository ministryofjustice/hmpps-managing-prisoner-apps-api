ALTER TABLE if exists response
    ADD column if not exists app uuid;
UPDATE response
SET app = (SELECT app_responses.app_id FROM app_responses WHERE response.id = app_responses.responses);
