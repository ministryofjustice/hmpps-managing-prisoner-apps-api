create table public.groups
(
    id               uuid not null
        primary key,
    establishment_id varchar(255),
    name             varchar(255),
    type             varchar(255)
        constraint groups_type_check
            check ((type)::text = ANY ((ARRAY ['WING'::character varying, 'DEPARTMENT'::character varying])::text[]))
    );

create table public.groups_initials_apps
(
    groups_id     uuid not null
        constraint fkpyja8nonimdxtsrtkc79a2h06
            references public.groups,
    initials_apps jsonb
);

