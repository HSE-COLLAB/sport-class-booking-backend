create table teachers
(
    user_id UUID not null,
    position varchar(255),
    constraint pk_teachers primary key (user_id),
    constraint fk_teachers_users foreign key (user_id) references users(id)
);