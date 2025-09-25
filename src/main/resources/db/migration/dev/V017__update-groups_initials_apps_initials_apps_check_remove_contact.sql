ALTER TABLE if exists groups_initials_apps DROP CONSTRAINT if exists groups_initials_apps_initials_apps_check;

ALTER TABLE if exists groups_initials_apps ADD CONSTRAINT groups_initials_apps_initials_apps_check
    check (
    (initials_apps)::text = ANY
    (ARRAY
    [
    ('PIN_PHONE_EMERGENCY_CREDIT_TOP_UP':: character varying)::text,
    ('PIN_PHONE_ADD_NEW_SOCIAL_CONTACT':: character varying)::text,
    ('PIN_PHONE_ADD_NEW_LEGAL_CONTACT':: character varying)::text,
    ('PIN_PHONE_REMOVE_CONTACT':: character varying)::text,
    ('PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS':: character varying)::text,
    ('PIN_PHONE_SUPPLY_LIST_OF_CONTACTS':: character varying)::text
    ]
    )
    );
