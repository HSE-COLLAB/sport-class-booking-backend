ALTER TABLE lessons DROP CONSTRAINT IF EXISTS fk_lessons_campus;
ALTER TABLE lessons DROP COLUMN IF EXISTS campus_id;

ALTER TABLE teachers DROP CONSTRAINT IF EXISTS fk_teachers_campus;
ALTER TABLE teachers DROP COLUMN IF EXISTS campus_id;

ALTER TABLE students DROP CONSTRAINT IF EXISTS fk_students_campus;
ALTER TABLE students DROP COLUMN IF EXISTS campus_id;