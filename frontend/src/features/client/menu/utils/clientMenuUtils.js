import { ALL_FILTER_VALUE } from "../constants/clientMenuConstants";

export function getProductCategories(products) {
  return [
    ALL_FILTER_VALUE,
    ...new Set(
      products.map((product) => product.category?.name).filter(Boolean),
    ),
  ];
}

export function filterMenuProducts(products, filters) {
  const {
    selectedCategory,
    maxPrice,
    onlyAvailable,
    onlyVegetarian,
    onlyVegan,
    selectedMeatType,
  } = filters;

  return products.filter((product) => {
    const matchesCategory =
      selectedCategory === ALL_FILTER_VALUE ||
      product.category?.name === selectedCategory;

    const matchesPrice =
      maxPrice === "" || Number(product.price) <= Number(maxPrice);

    const matchesAvailability = !onlyAvailable || product.available;

    const matchesVegetarian = !onlyVegetarian || product.vegetarian === true;

    const matchesVegan = !onlyVegan || product.vegan === true;

    const matchesMeatType =
      selectedMeatType === ALL_FILTER_VALUE ||
      product.meatType === selectedMeatType;

    return (
      matchesCategory &&
      matchesPrice &&
      matchesAvailability &&
      matchesVegetarian &&
      matchesVegan &&
      matchesMeatType
    );
  });
}
