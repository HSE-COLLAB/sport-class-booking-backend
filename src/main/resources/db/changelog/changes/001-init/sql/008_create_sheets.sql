create table sheets
(
    id UUID not null,
    lesson_id UUID not null,
    student_id UUID not null,
    visited bool,
    constraint pk_sheets primary key (id),
    constraint fk_sheets_lessons foreign key (lesson_id) references lessons(id),
    constraint fk_sheets_students foreign key (student_id) references students(user_id)
);