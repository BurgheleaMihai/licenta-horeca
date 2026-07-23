export const initialFormData = {
  productSelection: "NEW",
  name: "",
  variantType: "NONE",
  variantName: "",
  specificationValue: "",
  specificationUnit: "MILLILITER",
  stockType: "AUXILIARY",
  category: "PACKAGING",
  baseUnit: "PIECE",
  currentQuantity: "0",
  minimumQuantity: "0",
  active: true,
};

export const stockTypeLabels = {
  AUXILIARY: "Auxiliar",
  WAREHOUSE: "Depozit",
  FRUIT_AND_VEGETABLE: "Fructe si legume",
};

export const categoryLabels = {
  PACKAGING: "Ambalaje",
  CONSUMABLE: "Consumabile",
  BEVERAGE_INGREDIENT: "Ingrediente pentru bauturi",
  MEAT: "Carne",
  DAIRY: "Lactate",
  DRY_PRODUCT: "Produse uscate",
  FROZEN_PRODUCT: "Produse congelate",
  SAUCE: "Sosuri",
  OTHER_INGREDIENT: "Alte ingrediente",
  FRUIT: "Fructe",
  VEGETABLE: "Legume",
  OTHER: "Altele",
};

export const unitLabels = {
  PIECE: "buc",
  GRAM: "g",
  KILOGRAM: "kg",
  MILLILITER: "ml",
  LITER: "l",
};

export const variantTypeLabels = {
  NONE: "Fara varianta",
  MEASUREMENT: "Varianta numerica",
  TEXT: "Varianta text",
};

export const stockTypes = ["AUXILIARY", "WAREHOUSE", "FRUIT_AND_VEGETABLE"];

export const measurementUnits = [
  "PIECE",
  "GRAM",
  "KILOGRAM",
  "MILLILITER",
  "LITER",
];

export const categoriesByStockType = {
  AUXILIARY: ["PACKAGING", "CONSUMABLE", "BEVERAGE_INGREDIENT", "OTHER"],

  WAREHOUSE: [
    "MEAT",
    "DAIRY",
    "DRY_PRODUCT",
    "FROZEN_PRODUCT",
    "SAUCE",
    "OTHER_INGREDIENT",
    "OTHER",
  ],

  FRUIT_AND_VEGETABLE: ["FRUIT", "VEGETABLE", "OTHER"],
};
