ALTER TABLE if exists app DROP CONSTRAINT if exists app_app_type_check;

ALTER TABLE if exists app ADD CONSTRAINT app_app_type_check check ((app_type)::text = ANY((ARRAY ['PIN_PHONE_EMERGENCY_CREDIT_TOP_UP'::character varying, 'PIN_PHONE_ADD_NEW_SOCIAL_CONTACT'::character varying, 'PIN_PHONE_CREDIT_SWAP_VISITING_ORDERS'::character varying, 'PIN_PHONE_SUPPLY_LIST_OF_CONTACTS'::character varying])::text[]));
