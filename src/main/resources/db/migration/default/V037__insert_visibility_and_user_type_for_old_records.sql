UPDATE comment
set visibility = 'STAFF_ONLY' where visibility IS NULL;

UPDATE comment
set created_by_user_type = 'STAFF' where created_by_user_type IS NULL;

