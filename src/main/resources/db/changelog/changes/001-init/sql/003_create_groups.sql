CREATE TABLE groups (
    id UUID NOT NULL,
    faculty VARCHAR(255) NOT NULL,
    academic_major VARCHAR(255) NOT NULL,
    group_number VARCHAR(255) NOT NULL,
    CONSTRAINT pk_groups PRIMARY KEY (id)
);