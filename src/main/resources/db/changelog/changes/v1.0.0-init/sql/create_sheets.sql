CREATE TABLE sheets
(
    id         UUID NOT NULL,
    lesson_id  UUID NOT NULL,
    student_id UUID NOT NULL,
    visited    BOOLEAN,
    CONSTRAINT pk_sheets PRIMARY KEY (id),
    CONSTRAINT fk_sheets_lessons FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT fk_sheets_students FOREIGN KEY (student_id) REFERENCES students (user_id)
);