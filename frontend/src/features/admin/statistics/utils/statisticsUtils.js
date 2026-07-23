/*
 * Transforma o data intr-o valoare locala
 * compatibila cu inputurile HTML de tip date.
 */
export const getLocalDateValue = (date) => {
  const timezoneOffset = date.getTimezoneOffset() * 60 * 1000;

  return new Date(date.getTime() - timezoneOffset).toISOString().split("T")[0];
};

/*
 * Transforma o valoare de forma HH:mm
 * in numarul total de minute.
 */
const getMinutesFromTime = (timeValue) => {
  const [hours, minutes] = timeValue.split(":").map(Number);

  return hours * 60 + minutes;
};

/*
 * Stabileste intervalul calendaristic aferent
 * perioadei selectate de administrator.
 */
export const getPeriodDateRange = (filters) => {
  const today = new Date();

  if (filters.period === "TODAY") {
    const todayValue = getLocalDateValue(today);

    return {
      startDate: todayValue,
      endDate: todayValue,
    };
  }

  if (filters.period === "LAST_7_DAYS") {
    const startDate = new Date(today);

    startDate.setDate(today.getDate() - 6);

    return {
      startDate: getLocalDateValue(startDate),
      endDate: getLocalDateValue(today),
    };
  }

  if (filters.period === "CURRENT_MONTH") {
    const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);

    return {
      startDate: getLocalDateValue(firstDayOfMonth),
      endDate: getLocalDateValue(today),
    };
  }

  return {
    startDate: filters.customStartDate,
    endDate: filters.customEndDate,
  };
};

/*
 * Verifica daca un timestamp se incadreaza
 * in perioada si intervalul orar selectate.
 */
export const isTimestampInFilters = (timestamp, filters) => {
  if (!timestamp) {
    return false;
  }

  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return false;
  }

  const dateValue = getLocalDateValue(date);

  const { startDate, endDate } = getPeriodDateRange(filters);

  if (dateValue < startDate || dateValue > endDate) {
    return false;
  }

  const timestampMinutes = date.getHours() * 60 + date.getMinutes();

  const startMinutes = getMinutesFromTime(filters.startTime);

  const endMinutes = getMinutesFromTime(filters.endTime);

  return timestampMinutes >= startMinutes && timestampMinutes <= endMinutes;
};

/*
 * Genereaza denumirea perioadei afisate
 * in rezumatul filtrelor.
 */
export const getPeriodLabel = (filters) => {
  if (filters.period === "TODAY") {
    return "Astazi";
  }

  if (filters.period === "LAST_7_DAYS") {
    return "Ultimele 7 zile";
  }

  if (filters.period === "CURRENT_MONTH") {
    return "Luna curenta";
  }

  return `${filters.customStartDate} - ` + `${filters.customEndDate}`;
};

/*
 * Calculeaza indicatorii statistici pentru
 * perioada si intervalul orar aplicate.
 */
export const calculateStatistics = (orders, feedbackList, appliedFilters) => {
  const filteredOrders = orders.filter((order) =>
    isTimestampInFilters(order.createdAt, appliedFilters),
  );

  const servedOrders = orders.filter(
    (order) =>
      order.status === "SERVITA" &&
      isTimestampInFilters(
        order.completedAt || order.createdAt,
        appliedFilters,
      ),
  );

  const cancelledOrders = filteredOrders.filter(
    (order) => order.status === "ANULATA",
  );

  const filteredFeedback = feedbackList.filter((feedback) =>
    isTimestampInFilters(feedback.createdAt, appliedFilters),
  );

  const sales = servedOrders.reduce(
    (sum, order) => sum + Number(order.totalPrice || 0),
    0,
  );

  const averageOrderValue =
    servedOrders.length === 0 ? 0 : sales / servedOrders.length;

  const averageRating =
    filteredFeedback.length === 0
      ? 0
      : filteredFeedback.reduce(
          (sum, feedback) => sum + Number(feedback.rating || 0),
          0,
        ) / filteredFeedback.length;

  const productSales = {};

  servedOrders.forEach((order) => {
    order.items?.forEach((item) => {
      const productName = item.product?.name || "Produs necunoscut";

      if (!productSales[productName]) {
        productSales[productName] = 0;
      }

      productSales[productName] += Number(item.quantity || 0);
    });
  });

  const productSalesList = Object.entries(productSales)
    .map(([name, quantity]) => ({
      name,
      quantity,
    }))
    .sort(
      (firstProduct, secondProduct) =>
        secondProduct.quantity - firstProduct.quantity,
    );

  const totalProductsSold = productSalesList.reduce(
    (sum, product) => sum + product.quantity,
    0,
  );

  return {
    totalOrders: filteredOrders.length,
    servedOrders: servedOrders.length,
    cancelledOrders: cancelledOrders.length,
    sales,
    averageOrderValue,
    averageRating,
    totalProductsSold,
    productSalesList,
  };
};
