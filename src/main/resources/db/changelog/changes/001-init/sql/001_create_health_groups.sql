create table health_groups
(
    id integer not null,
    description varchar(255) not null,
    constraint pk_health_groups primary key (id)
);

insert into health_groups (id, description)
values (1, 'Основная группа'),
       (2, 'Подготовительная группа'),
       (3, 'Специальная группа А'),
       (4, 'Специальная группа Б'),
       (5, 'Нет данных');
