CREATE TABLE workout_types
(
    id        UUID         NOT NULL,
    title     VARCHAR(255) NOT NULL,
    is_active BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT pk_workout_types PRIMARY KEY (id)
);

CREATE TABLE workout_type_health_groups
(
    workout_type_id  UUID    NOT NULL,
    health_group_id  INTEGER NOT NULL,
    CONSTRAINT pk_workout_type_health_groups PRIMARY KEY (workout_type_id, health_group_id),
    CONSTRAINT fk_wthg_workout_type FOREIGN KEY (workout_type_id) REFERENCES workout_types (id) ON DELETE CASCADE,
    CONSTRAINT fk_wthg_health_group FOREIGN KEY (health_group_id) REFERENCES health_groups (id)
);