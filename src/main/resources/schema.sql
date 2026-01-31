SET @db := DATABASE();

SET @table_exists := (
  SELECT COUNT(*)
  FROM information_schema.TABLES t
  WHERE t.TABLE_SCHEMA = @db
    AND t.TABLE_NAME = 'assessment_scores'
);

SET @idx_user_only := (
  SELECT IF(
    @table_exists = 0,
    NULL,
    (
      SELECT s.INDEX_NAME
      FROM information_schema.STATISTICS s
      JOIN information_schema.TABLE_CONSTRAINTS tc
        ON tc.CONSTRAINT_SCHEMA = s.TABLE_SCHEMA
       AND tc.TABLE_NAME = s.TABLE_NAME
       AND tc.CONSTRAINT_NAME = s.INDEX_NAME
      WHERE s.TABLE_SCHEMA = @db
        AND s.TABLE_NAME = 'assessment_scores'
        AND tc.CONSTRAINT_TYPE = 'UNIQUE'
      GROUP BY s.INDEX_NAME
      HAVING GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) = 'user_id'
      LIMIT 1
    )
  )
);

SET @sql_drop_user_only := IF(
  @idx_user_only IS NULL OR @idx_user_only = 'PRIMARY',
  'SELECT 1',
  CONCAT('DROP INDEX `', @idx_user_only, '` ON `assessment_scores`')
);
PREPARE stmt_drop_user_only FROM @sql_drop_user_only;
EXECUTE stmt_drop_user_only;
DEALLOCATE PREPARE stmt_drop_user_only;

SET @has_user_course_unique := (
  SELECT IF(
    @table_exists = 0,
    1,
    (
      SELECT COUNT(*) FROM (
        SELECT s.INDEX_NAME
        FROM information_schema.STATISTICS s
        JOIN information_schema.TABLE_CONSTRAINTS tc
          ON tc.CONSTRAINT_SCHEMA = s.TABLE_SCHEMA
         AND tc.TABLE_NAME = s.TABLE_NAME
         AND tc.CONSTRAINT_NAME = s.INDEX_NAME
        WHERE s.TABLE_SCHEMA = @db
          AND s.TABLE_NAME = 'assessment_scores'
          AND tc.CONSTRAINT_TYPE = 'UNIQUE'
        GROUP BY s.INDEX_NAME
        HAVING GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) = 'user_id,course_id'
      ) t
    )
  )
);

SET @sql_create_user_course_unique := IF(
  @has_user_course_unique = 0,
  'CREATE UNIQUE INDEX `uk_assessment_scores_user_course_id` ON `assessment_scores` (`user_id`, `course_id`)',
  'SELECT 1'
);
PREPARE stmt_create_user_course_unique FROM @sql_create_user_course_unique;
EXECUTE stmt_create_user_course_unique;
DEALLOCATE PREPARE stmt_create_user_course_unique;
