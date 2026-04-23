ALTER TABLE sheets ADD CONSTRAINT uq_sheets_lesson_student UNIQUE (lesson_id, student_id);
