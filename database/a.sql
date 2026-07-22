USE horeca_db;

SELECT
    u.id,
    u.full_name,
    u.email,
    r.name AS role,
    CASE
        WHEN u.password LIKE '$2a$%'
          OR u.password LIKE '$2b$%'
          OR u.password LIKE '$2y$%'
        THEN 'BCRYPT'
        ELSE 'TEXT_CLAR'
    END AS password_format
FROM app_users u
JOIN roles r ON r.id = u.role_id
ORDER BY u.id;