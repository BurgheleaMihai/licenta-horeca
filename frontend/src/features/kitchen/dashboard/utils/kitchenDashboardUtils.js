import {
  BEVERAGE_CATEGORY_NAME,
  READY_ITEM_STATUS,
} from "../constants/kitchenDashboardConstants";

/*
 * Returneaza produsele care trebuie pregatite
 * in bucatarie si care nu sunt deja gata.
 */
export function getKitchenItems(order) {
  return (
    order.items?.filter(
      (item) =>
        item.product?.category?.name !== BEVERAGE_CATEGORY_NAME &&
        item.status !== READY_ITEM_STATUS,
    ) || []
  );
}

/*
 * Pastreaza doar comenzile care contin cel putin
 * un produs ce trebuie pregatit in bucatarie.
 */
export function getVisibleKitchenOrders(orders) {
  return orders.filter((order) => getKitchenItems(order).length > 0);
}
