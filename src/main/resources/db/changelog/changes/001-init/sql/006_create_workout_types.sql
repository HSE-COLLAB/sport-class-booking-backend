create table workout_types
(
    id UUID not null,
    title varchar(255),
    allow_health_group_id integer not null,
    is_active bool not null default true,
    constraint pk_workout_types primary key (id),
    constraint fk_workout_types_health_groups foreign key (allow_health_group_id) references health_groups(id)
);