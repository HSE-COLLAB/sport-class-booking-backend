CREATE TABLE health_groups
(
    id          SERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);
SELECT setval('health_groups_id_seq', 5);