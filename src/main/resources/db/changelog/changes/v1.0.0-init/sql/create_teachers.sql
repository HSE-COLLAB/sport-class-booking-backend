CREATE TABLE teachers
(
    user_id  UUID NOT NULL,
    position VARCHAR(255),
    CONSTRAINT pk_teachers PRIMARY KEY (user_id),
    CONSTRAINT fk_teachers_users FOREIGN KEY (user_id) REFERENCES users (id)
);