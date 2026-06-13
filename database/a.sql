USE horeca_db;

SELECT id, status, created_at
FROM orders
ORDER BY created_at DESC;

SELECT id, active, started_at, ended_at
FROM table_sessions
ORDER BY started_at DESC;