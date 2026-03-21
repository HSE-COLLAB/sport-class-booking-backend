CREATE TABLE users
(
    id          UUID                NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255)        NOT NULL,
    first_name  VARCHAR(255)        NOT NULL,
    last_name   VARCHAR(255)        NOT NULL,
    middle_name VARCHAR(255),
    role        VARCHAR(255)        NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN'))
);