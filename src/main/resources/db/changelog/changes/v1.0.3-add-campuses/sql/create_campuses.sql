CREATE TABLE campuses
(
    id       INTEGER      NOT NULL,
    name     VARCHAR(255) NOT NULL,
    timezone VARCHAR(255) NOT NULL,
    CONSTRAINT pk_campuses PRIMARY KEY (id)
);