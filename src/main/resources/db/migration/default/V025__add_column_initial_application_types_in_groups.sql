create table if not exists groups_initials_application_types
(
    initials_application_types bigint,
    groups_id                  uuid not null
        constraint fk12y9882j9ci1tcmslpnauw1eb
            references groups
);
