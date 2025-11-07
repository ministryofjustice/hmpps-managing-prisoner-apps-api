UPDATE table_name
SET column_value = CASE column_name
                       WHEN 'column_name1' THEN column_value1
                       WHEN 'column_name2' THEN column_value2
                       ELSE column_value
    END
WHERE column_name IN('column_name1', 'column_name2');