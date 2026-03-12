CREATE TABLE lessons (
    id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    place VARCHAR(255) NOT NULL,
    workout_type_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    start_time timestamptz NOT NULL,
    end_time timestamptz NOT NULL,
    total_places INTEGER NOT NULL,
    notes TEXT,
    CONSTRAINT pk_lessons PRIMARY KEY (id),
    CONSTRAINT fk_lessons_workout_types FOREIGN KEY (workout_type_id) REFERENCES workout_types(id),
    CONSTRAINT fk_lessons_teachers FOREIGN KEY (teacher_id) REFERENCES teachers(user_id)
);