ALTER TABLE if exists app
    ADD column if not exists prisoner_messages_read_at timestamp(6);
