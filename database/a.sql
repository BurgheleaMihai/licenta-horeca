USE horeca_db;

UPDATE orders
SET status = 'IN_PREPARARE'
WHERE id = 4;

UPDATE order_items
SET status = 'IN_PREPARARE'
WHERE order_id = 4;

SELECT id, status
FROM orders
WHERE id = 4;

SELECT id, order_id, product_id, quantity, status
FROM order_items
WHERE order_id = 4;