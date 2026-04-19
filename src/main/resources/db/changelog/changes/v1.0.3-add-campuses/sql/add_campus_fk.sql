ALTER TABLE students ADD COLUMN campus_id INTEGER NOT NULL;
ALTER TABLE students ADD CONSTRAINT fk_students_campus FOREIGN KEY (campus_id) REFERENCES campuses (id);

ALTER TABLE teachers ADD COLUMN campus_id INTEGER NOT NULL;
ALTER TABLE teachers ADD CONSTRAINT fk_teachers_campus FOREIGN KEY (campus_id) REFERENCES campuses (id);

ALTER TABLE lessons ADD COLUMN campus_id INTEGER NOT NULL;
ALTER TABLE lessons ADD CONSTRAINT fk_lessons_campus FOREIGN KEY (campus_id) REFERENCES campuses (id);