import {
  BEVERAGE_CATEGORY_NAME,
  READY_ITEM_STATUS,
} from "../constants/barDashboardConstants";

/*
 * Returneaza bauturile care trebuie pregatite
 * la bar si care nu sunt deja gata.
 */
export function getBarItems(order) {
  return (
    order.items?.filter(
      (item) =>
        item.product?.category?.name === BEVERAGE_CATEGORY_NAME &&
        item.status !== READY_ITEM_STATUS,
    ) || []
  );
}

/*
 * Pastreaza doar comenzile care contin cel putin
 * o bautura ce trebuie pregatita la bar.
 */
export function getVisibleBarOrders(orders) {
  return orders.filter((order) => getBarItems(order).length > 0);
}
