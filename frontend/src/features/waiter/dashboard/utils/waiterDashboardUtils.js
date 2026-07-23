import { BEVERAGE_CATEGORY_NAME } from "../constants/waiterDashboardConstants";

export function findActiveSessionForTable(activeSessions, tableId) {
  return activeSessions.find(
    (session) => session.restaurantTable?.id === tableId,
  );
}

export function buildSelectedOrderItems(products, productQuantities) {
  return products
    .filter((product) => Number(productQuantities[product.id] || 0) > 0)
    .map((product) => ({
      productId: product.id,

      quantity: Number(productQuantities[product.id]),
    }));
}

export function getFoodItems(order) {
  return (
    order.items?.filter(
      (item) => item.product?.category?.name !== BEVERAGE_CATEGORY_NAME,
    ) || []
  );
}

export function getDrinkItems(order) {
  return (
    order.items?.filter(
      (item) => item.product?.category?.name === BEVERAGE_CATEGORY_NAME,
    ) || []
  );
}

export function getServerErrorMessage(error, fallbackMessage) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallbackMessage
  );
}
