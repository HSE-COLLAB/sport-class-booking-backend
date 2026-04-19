-- STUDENT GROUPS
INSERT INTO groups (id, faculty, academic_major, group_number)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ФКН', 'Программная инженерия',                    'ПИ-2024'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ФКН', 'Прикладная математика и информатика',     'ПМИ-2024'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ФБИ', 'Бизнес-информатика',                       'БИ-2024');

-- USERS
-- Пароль для всех пользователей: "test"
INSERT INTO users (id, email, password, first_name, last_name, middle_name, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin1@mail.ru',   '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Admin1',   'A.', 'S.', 'ADMIN'),

    ('33333333-3333-3333-3333-333333333333', 'teacher1@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Teacher1', 'A.', 'S.', 'TEACHER'),
    ('44444444-4444-4444-4444-444444444444', 'teacher2@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Teacher2', 'B.', 'K.', 'TEACHER'),
    ('77777777-7777-7777-7777-777777777777', 'teacher3@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Teacher3', 'C.', 'M.', 'TEACHER'),

    ('55555555-5555-5555-5555-555555555555', 'student1@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Student1', 'A.', 'S.', 'STUDENT');

-- STUDENTS
-- Student1 (55555555) — главный тест-студент из DevAuthFilter
INSERT INTO students (user_id, group_id, health_group_id, campus_id)
VALUES
    ('55555555-5555-5555-5555-555555555555', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1, 3);

-- TEACHERS в разных кампусах для тестирования фильтров
-- Teacher1 (33333333) — главный тест-преподаватель из DevAuthFilter (Нижний Новгород)
INSERT INTO teachers (user_id, position, campus_id)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'Профессор', 3), -- Нижний Новгород
    ('44444444-4444-4444-4444-444444444444', 'Доцент',    1), -- Москва
    ('77777777-7777-7777-7777-777777777777', 'Профессор', 4); -- Пермь

-- WORKOUT TYPES
INSERT INTO workout_types (id, title, is_active)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Волейбол',  true),
    ('10000000-0000-0000-0000-000000000002', 'Баскетбол', true),
    ('10000000-0000-0000-0000-000000000003', 'Пилатес',   true),
    ('10000000-0000-0000-0000-000000000004', 'Шахматы',   true);

-- WORKOUT_TYPE <-> HEALTH_GROUPS (M2M)
-- Волейбол: основная (1), подготовительная (2)
-- Баскетбол: основная (1), подготовительная (2)
-- Пилатес: основная (1), подготовительная (2), специальная А (3)
-- Шахматы: все кроме "нет данных" (1, 2, 3, 4)
INSERT INTO workout_type_health_groups (workout_type_id, health_group_id)
VALUES
    ('10000000-0000-0000-0000-000000000001', 1),
    ('10000000-0000-0000-0000-000000000001', 2),

    ('10000000-0000-0000-0000-000000000002', 1),
    ('10000000-0000-0000-0000-000000000002', 2),

    ('10000000-0000-0000-0000-000000000003', 1),
    ('10000000-0000-0000-0000-000000000003', 2),
    ('10000000-0000-0000-0000-000000000003', 3),

    ('10000000-0000-0000-0000-000000000004', 1),
    ('10000000-0000-0000-0000-000000000004', 2),
    ('10000000-0000-0000-0000-000000000004', 3),
    ('10000000-0000-0000-0000-000000000004', 4);

-- LESSONS (разные время/кампусы/преподаватели/места для тестирования фильтров)
INSERT INTO lessons (id, title, place, workout_type_id, teacher_id, campus_id, start_time, end_time, total_places, notes)
VALUES
    -- UPCOMING: завтра, Teacher1 (НН), Волейбол
    ('20000000-0000-0000-0000-000000000001', 'Волейбол утро',
     'Зал 1',
     '10000000-0000-0000-0000-000000000001',
     '33333333-3333-3333-3333-333333333333',
     3,
     NOW() + INTERVAL '1 day' + INTERVAL '10 hours',
     NOW() + INTERVAL '1 day' + INTERVAL '11 hours 30 minutes',
     20,
     'Занятие для начинающих'),

    -- UPCOMING: через 2 дня, Teacher1 (НН), Баскетбол
    ('20000000-0000-0000-0000-000000000002', 'Баскетбол',
     'Зал 2',
     '10000000-0000-0000-0000-000000000002',
     '33333333-3333-3333-3333-333333333333',
     3,
     NOW() + INTERVAL '2 days' + INTERVAL '14 hours',
     NOW() + INTERVAL '2 days' + INTERVAL '15 hours',
     15,
     null),

    -- UPCOMING: через 3 дня, Teacher2 (Москва), Пилатес
    ('20000000-0000-0000-0000-000000000003', 'Пилатес вечер',
     'Зал пилатеса',
     '10000000-0000-0000-0000-000000000003',
     '44444444-4444-4444-4444-444444444444',
     1,
     NOW() + INTERVAL '3 days' + INTERVAL '18 hours',
     NOW() + INTERVAL '3 days' + INTERVAL '19 hours 30 minutes',
     10,
     'Взять коврик'),

    -- UPCOMING: через 5 дней, Teacher3 (Пермь), Шахматы
    ('20000000-0000-0000-0000-000000000004', 'Шахматный клуб',
     'Аудитория 305',
     '10000000-0000-0000-0000-000000000004',
     '77777777-7777-7777-7777-777777777777',
     4,
     NOW() + INTERVAL '5 days' + INTERVAL '16 hours',
     NOW() + INTERVAL '5 days' + INTERVAL '17 hours',
     8,
     null),

    -- PAST: вчера, Teacher1 (НН), Волейбол
    ('20000000-0000-0000-0000-000000000005', 'Волейбол (прошедший)',
     'Зал 1',
     '10000000-0000-0000-0000-000000000001',
     '33333333-3333-3333-3333-333333333333',
     3,
     NOW() - INTERVAL '1 day' + INTERVAL '10 hours',
     NOW() - INTERVAL '1 day' + INTERVAL '11 hours 30 minutes',
     20,
     null);
