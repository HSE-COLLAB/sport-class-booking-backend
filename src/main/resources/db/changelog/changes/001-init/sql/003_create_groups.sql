create table groups
(
    id UUID not null,
    faculty varchar(255) not null,
    academic_major varchar(255) not null,
    group_number varchar(255) not null,
    constraint pk_groups primary key (id)
);