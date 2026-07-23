/*
 * Returneaza data curenta in formatul
 * folosit de inputurile HTML de tip date.
 */
export const getCurrentDateValue = () => {
  const currentDate = new Date();

  const timezoneOffset = currentDate.getTimezoneOffset() * 60 * 1000;

  return new Date(currentDate.getTime() - timezoneOffset)
    .toISOString()
    .split("T")[0];
};

/*
 * Calculeaza numarul comenzilor active
 * pentru fiecare status operational.
 */
export const getOperationalOrderCounts = (activeOrders) => {
  return {
    newOrdersCount: activeOrders.filter((order) => order.status === "NOUA")
      .length,

    inPreparationOrdersCount: activeOrders.filter(
      (order) => order.status === "IN_PREPARARE",
    ).length,

    readyOrdersCount: activeOrders.filter((order) => order.status === "GATA")
      .length,
  };
};

/*
 * Calculeaza cantitatea vanduta pentru
 * fiecare produs din comenzile servite.
 */
export const getProductSalesList = (allOrders) => {
  const servedOrders = allOrders.filter((order) => order.status === "SERVITA");

  const productSales = {};

  servedOrders.forEach((order) => {
    order.items?.forEach((item) => {
      const productName = item.product?.name || "Produs necunoscut";

      if (!productSales[productName]) {
        productSales[productName] = 0;
      }

      productSales[productName] += Number(item.quantity);
    });
  });

  return Object.entries(productSales)
    .map(([name, quantity]) => ({
      name,
      quantity,
    }))
    .sort(
      (firstProduct, secondProduct) =>
        secondProduct.quantity - firstProduct.quantity,
    );
};
