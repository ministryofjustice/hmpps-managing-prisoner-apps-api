create table if not exists app_file
(
    id           uuid not null
        primary key,
    document_id  varchar(255),
    file_name    varchar(255),
    file_type    varchar(255),
    created_by   varchar(255),
    created_date timestamp(6),
    app_id       uuid
        constraint fk7m4p5ywviu00eojd5ginirc6x
            references app
);
