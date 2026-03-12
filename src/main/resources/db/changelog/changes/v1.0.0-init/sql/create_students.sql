CREATE TABLE students
(
    user_id         UUID    NOT NULL,
    group_id        UUID,
    health_group_id INTEGER NOT NULL DEFAULT 5,
    CONSTRAINT pk_students PRIMARY KEY (user_id),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_students_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_students_health_group FOREIGN KEY (health_group_id) REFERENCES health_groups (id)
);