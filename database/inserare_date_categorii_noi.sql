USE horeca_db;

UPDATE products
SET allergens = 'gluten,lactoza',
    vegetarian = 1,
    vegan = 0,
    meat_type = 'none'
WHERE id = 1;

UPDATE products
SET allergens = 'gluten,lactoza',
    vegetarian = 0,
    vegan = 0,
    meat_type = 'porc'
WHERE id = 2;

UPDATE products
SET allergens = 'gluten,lactoza,oua',
    vegetarian = 0,
    vegan = 0,
    meat_type = 'porc'
WHERE id = 3;

UPDATE products
SET allergens = 'gluten,lactoza',
    vegetarian = 0,
    vegan = 0,
    meat_type = 'vita'
WHERE id = 4;

UPDATE products
SET allergens = '',
    vegetarian = 1,
    vegan = 1,
    meat_type = 'none'
WHERE id = 5;

UPDATE products
SET allergens = '',
    vegetarian = 1,
    vegan = 1,
    meat_type = 'none'
WHERE id = 6;

UPDATE products
SET allergens = 'lactoza,oua',
    vegetarian = 1,
    vegan = 0,
    meat_type = 'none'
WHERE id = 7;

UPDATE products
SET allergens = 'gluten,lactoza,oua',
    vegetarian = 1,
    vegan = 0,
    meat_type = 'none'
WHERE id = 8;

SELECT id, name, allergens, vegetarian, vegan, meat_type
FROM products;