-- STUDENT GROUPS x2
INSERT INTO groups (id, faculty, academic_major, group_number)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Computer Science', 'Software Engineering', 'CST-2024'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Mathematics', 'Applied Mathematics', 'FM-2024');

-- USERS (ADMIN, TEACHER, STUDENT) x2
-- Пароль для пользователей: "test"
INSERT INTO users (id, email, password, first_name, last_name, middle_name, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin1@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Admin1', 'A.', 'S.', 'ADMIN'),
    ('22222222-2222-2222-2222-222222222222', 'admin2@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Admin2', 'A.', 'S.', 'ADMIN'),

    ('33333333-3333-3333-3333-333333333333', 'teacher1@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Teacher1', 'A.', 'S.', 'TEACHER'),
    ('44444444-4444-4444-4444-444444444444', 'teacher2@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Teacher2', 'A.', 'S.', 'TEACHER'),

    ('55555555-5555-5555-5555-555555555555', 'student1@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Student1', 'A.', 'S.', 'STUDENT'),
    ('66666666-6666-6666-6666-666666666666', 'student2@mail.ru', '$2a$10$ESaaWK8//HirdHPjwQd9AePrMhUIzI5WbH1hMWqKgdFsYIeclK42m', 'Student2', 'A.', 'S.', 'STUDENT');

-- STUDENTS x2
INSERT INTO students (user_id, group_id, health_group_id)
VALUES
    ('55555555-5555-5555-5555-555555555555', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1),
    ('66666666-6666-6666-6666-666666666666', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 2);

-- TEACHERS x2
INSERT INTO teachers (user_id, position)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'Professor'),
    ('44444444-4444-4444-4444-444444444444', 'Docent');