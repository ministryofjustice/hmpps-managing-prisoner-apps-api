create table if not exists application_type
(
    id                   bigint  not null
    primary key,
    name                 varchar(255),
    generic_type         boolean not null,
    log_detail_required  boolean not null,
    application_group_id bigint
        constraint fk42vx8saf15ghsj9i88i6i49fg
            references application_group
);
