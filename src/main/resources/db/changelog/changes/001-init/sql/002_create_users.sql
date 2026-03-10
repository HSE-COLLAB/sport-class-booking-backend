CREATE TABLE users
(
    id UUID not null,
    email varchar(255) unique not null,
    password varchar(255) not null,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    middle_name varchar(255),
    constraint pk_users primary key (id)
);