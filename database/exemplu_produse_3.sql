USE horeca_db;

INSERT INTO categories (name, description, active)
VALUES 
('Pizza', 'Produse din categoria pizza', 1),
('Paste', 'Produse din categoria paste', 1),
('Bauturi', 'Bauturi reci si calde', 1),
('Deserturi', 'Deserturi disponibile in meniu', 1);

INSERT INTO products (name, description, price, available, category_id)
VALUES
('Pizza Margherita', 'Pizza cu sos de rosii, mozzarella si busuioc', 32.00, 1, 1),
('Pizza Diavola', 'Pizza cu salam picant, mozzarella si sos de rosii', 38.00, 1, 1),
('Paste Carbonara', 'Paste cu sos alb, bacon si parmezan', 35.00, 1, 2),
('Paste Bolognese', 'Paste cu sos ragu si parmezan', 36.00, 1, 2),
('Apa plata', 'Apa plata 500 ml', 8.00, 1, 3),
('Limonada', 'Limonada cu lamaie si menta', 15.00, 1, 3),
('Tiramisu', 'Desert italian cu mascarpone si cafea', 22.00, 1, 4),
('Papanasi', 'Papanasi cu smantana si dulceata', 24.00, 0, 4);