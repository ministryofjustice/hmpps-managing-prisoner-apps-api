update comment set visibility = 'STAFF_ONLY' WHERE COALESCE(visibility, '') = '';

update comment set created_by_user_type = 'STAFF' WHERE COALESCE(created_by_user_type, '') = '';

