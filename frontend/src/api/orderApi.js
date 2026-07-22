import apiClient from "./apiClient";

const API_URL = "/api/orders";

export const getAllOrders = () => {
  return apiClient.get(API_URL);
};

export const getActiveOrders = () => {
  return apiClient.get(`${API_URL}/active`);
};

export const getTodayOrderStatistics = () => {
  return apiClient.get(`${API_URL}/statistics/today`);
};

export const getOrderStatistics = (
  date,
  startTime,
  endTime
) => {
  return apiClient.get(`${API_URL}/statistics`, {
    params: {
      date,
      startTime,
      endTime,
    },
  });
};

export const updateOrderStatus = (
  orderId,
  status
) => {
  return apiClient.put(
    `${API_URL}/${orderId}/status`,
    {
      status,
    }
  );
};

export const createOrder = (order) => {
  return apiClient.post(API_URL, order);
};

export const getKitchenOrders = () => {
  return apiClient.get(`${API_URL}/kitchen`);
};

export const getBarOrders = () => {
  return apiClient.get(`${API_URL}/bar`);
};

export const updateOrderItemStatus = (
  itemId,
  status
) => {
  return apiClient.put(
    `${API_URL}/items/${itemId}/status`,
    {
      status,
    }
  );
};
