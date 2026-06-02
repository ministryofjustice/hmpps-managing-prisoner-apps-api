UPDATE comment
SET
    visibility = CASE WHEN COALESCE(visibility, '') = '' THEN 'STAFF_ONLY' ELSE visibility END,
    created_by_user_type = CASE WHEN COALESCE(created_by_user_type, '') = '' THEN 'STAFF' ELSE created_by_user_type END
WHERE COALESCE(visibility, '') = '' OR COALESCE(created_by_user_type, '') = '';

