create table if not exists public.groups
(
    id               uuid not null
    primary key,
    establishment_id varchar(255),
    name             varchar(255),
    type             varchar(255)
    constraint groups_type_check
    check ((type)::text = ANY ((ARRAY ['WING'::character varying, 'DEPARTMENT'::character varying])::text[]))
    );

create table if not exists public.groups_initials_apps
(
    groups_id     uuid not null
    constraint fkpyja8nonimdxtsrtkc79a2h06
    references public.groups,
    initials_apps varchar(255)
    constraint groups_initials_apps_initials_apps_check
    check ((initials_apps)::text = ANY
((ARRAY ['PIN_PHONE_CREDIT_TOP_UP'::character varying, 'PIN_PHONE_EMERGENCY_CREDIT_TOP_UP'::character varying, 'PIN_PHONE_ADD_NEW_CONTACT'::character varying, 'PIN_PHONE_REMOVE_CONTACT'::character varying, 'PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS'::character varying, 'PIN_PHONE_SUPPLY_LIST_OF_CONTACTS'::character varying])::text[]))
    );

