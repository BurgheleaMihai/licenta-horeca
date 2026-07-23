/*
 * Formateaza un timestamp pentru afisarea
 * in interfata administratorului.
 */
export const formatDateTime = (value) => {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return date.toLocaleString("ro-RO");
};

/*
 * Extrage numarul mesei asociate comenzii.
 */
export const getTableNumber = (order) => {
  return order.tableSession?.restaurantTable?.tableNumber ?? null;
};

/*
 * Verifica daca o comanda apartine istoricului,
 * adica este servita sau anulata.
 */
export const isClosedOrder = (order) => {
  return order.status === "SERVITA" || order.status === "ANULATA";
};

/*
 * Extrage si sorteaza numerele meselor care au
 * comenzi inchise.
 */
export const getClosedOrderTableNumbers = (orders) => {
  return [
    ...new Set(
      orders
        .filter(isClosedOrder)
        .map((order) => getTableNumber(order))
        .filter((tableNumber) => tableNumber !== null),
    ),
  ].sort((firstNumber, secondNumber) => firstNumber - secondNumber);
};

/*
 * Valideaza filtrele introduse pentru cautarea
 * in istoricul comenzilor.
 *
 * Returneaza un mesaj de eroare sau un sir gol.
 */
export const validateOrderHistoryFilters = (filters) => {
  const hasOnlyOneDate =
    Boolean(filters.startDate) !== Boolean(filters.endDate);

  if (hasOnlyOneDate) {
    return "Completeaza atat data de inceput, " + "cat si data de sfarsit.";
  }

  if (filters.startDate && filters.endDate) {
    const startDateTime = new Date(
      `${filters.startDate}T${filters.startTime}:00`,
    );

    const endDateTime = new Date(
      `${filters.endDate}T${filters.endTime}:59.999`,
    );

    if (
      Number.isNaN(startDateTime.getTime()) ||
      Number.isNaN(endDateTime.getTime())
    ) {
      return "Intervalul selectat nu este valid.";
    }

    if (endDateTime < startDateTime) {
      return "Sfarsitul intervalului trebuie sa fie " + "dupa inceput.";
    }
  }

  const minimumValue =
    filters.minValue === "" ? null : Number(filters.minValue);

  const maximumValue =
    filters.maxValue === "" ? null : Number(filters.maxValue);

  if (
    minimumValue !== null &&
    (Number.isNaN(minimumValue) || minimumValue < 0)
  ) {
    return "Valoarea minima trebuie sa fie zero " + "sau mai mare.";
  }

  if (
    maximumValue !== null &&
    (Number.isNaN(maximumValue) || maximumValue < 0)
  ) {
    return "Valoarea maxima trebuie sa fie zero " + "sau mai mare.";
  }

  if (
    minimumValue !== null &&
    maximumValue !== null &&
    maximumValue < minimumValue
  ) {
    return (
      "Valoarea maxima trebuie sa fie mai mare " + "decat valoarea minima."
    );
  }

  return "";
};

/*
 * Pastreaza doar comenzile inchise si le sorteaza
 * descrescator dupa data crearii.
 */
export const getSortedClosedOrders = (orders) => {
  return orders.filter(isClosedOrder).sort((firstOrder, secondOrder) => {
    return (
      new Date(secondOrder.createdAt).getTime() -
      new Date(firstOrder.createdAt).getTime()
    );
  });
};

/*
 * Filtreaza comenzile inchise dupa criteriile
 * aplicate de administrator.
 */
export const filterClosedOrders = (sortedClosedOrders, appliedFilters) => {
  let startDateTime = null;
  let endDateTime = null;

  if (appliedFilters.startDate && appliedFilters.endDate) {
    startDateTime = new Date(
      `${appliedFilters.startDate}T${appliedFilters.startTime}:00`,
    );

    endDateTime = new Date(
      `${appliedFilters.endDate}T${appliedFilters.endTime}:59.999`,
    );
  }

  const minimumValue =
    appliedFilters.minValue === "" ? null : Number(appliedFilters.minValue);

  const maximumValue =
    appliedFilters.maxValue === "" ? null : Number(appliedFilters.maxValue);

  return sortedClosedOrders.filter((order) => {
    if (
      appliedFilters.status !== "ALL_CLOSED" &&
      order.status !== appliedFilters.status
    ) {
      return false;
    }

    const tableNumber = getTableNumber(order);

    if (
      appliedFilters.tableNumber !== "ALL" &&
      String(tableNumber) !== appliedFilters.tableNumber
    ) {
      return false;
    }

    const orderValue = Number(order.totalPrice ?? 0);

    if (minimumValue !== null && orderValue < minimumValue) {
      return false;
    }

    if (maximumValue !== null && orderValue > maximumValue) {
      return false;
    }

    if (startDateTime && endDateTime) {
      const createdAt = new Date(order.createdAt);

      if (
        Number.isNaN(createdAt.getTime()) ||
        createdAt < startDateTime ||
        createdAt > endDateTime
      ) {
        return false;
      }
    }

    return true;
  });
};

/*
 * Stabileste lista afisata in functie de modul
 * selectat: cautare, comenzi recente sau nimic.
 */
export const getDisplayedOrders = (
  resultsMode,
  sortedClosedOrders,
  filteredOrders,
) => {
  if (resultsMode === "RECENT") {
    return sortedClosedOrders.slice(0, 5);
  }

  if (resultsMode === "FILTERED") {
    return filteredOrders;
  }

  return [];
};

/*
 * Calculeaza valoarea totala a comenzilor
 * afisate.
 */
export const calculateDisplayedOrdersValue = (displayedOrders) => {
  return displayedOrders.reduce(
    (sum, order) => sum + Number(order.totalPrice ?? 0),
    0,
  );
};
