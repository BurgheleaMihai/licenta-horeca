USE horeca_db;

UPDATE products
SET available = 1
WHERE id = 8;

SELECT id, name, available
FROM products
WHERE id = 8;