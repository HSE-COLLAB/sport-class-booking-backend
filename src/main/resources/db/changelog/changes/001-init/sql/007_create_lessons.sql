create table lessons
(
    id UUID not null,
    title varchar(255) not null,
    place varchar(255) not null,
    workout_type_id UUID not null,
    teacher_id UUID not null,
    start_time timestamptz not null,
    end_time timestamptz not null,
    total_places integer not null,
    notes text,
    constraint pk_lessons primary key (id),
    constraint fk_lessons_workout_types foreign key (workout_type_id) references workout_types(id),
    constraint fk_lessons_teachers foreign key (teacher_id) references teachers(user_id)
);