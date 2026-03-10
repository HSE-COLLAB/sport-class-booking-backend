create table students
(
    user_id UUID not null,
    group_id UUID not null,
    health_group_id integer not null default 5,
    constraint pk_students primary key (user_id),
    constraint fk_students_user foreign key (user_id) references users(id),
    constraint fk_students_group foreign key (group_id) references groups(id),
    constraint fk_students_health_group foreign key (health_group_id) references health_groups(id)
);