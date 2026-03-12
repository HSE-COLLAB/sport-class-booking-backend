CREATE TABLE workout_types (
    id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    allow_health_group_id INTEGER NOT NULL,
    is_active bool NOT NULL DEFAULT true,
    CONSTRAINT pk_workout_types PRIMARY KEY (id),
    CONSTRAINT fk_workout_types_health_groups FOREIGN KEY (allow_health_group_id) REFERENCES health_groups(id)
);