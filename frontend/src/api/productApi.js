import apiClient from "./apiClient";

const API_URL = "/api/products";
const FEEDBACK_URL = "/api/feedback";
const ORDERS_URL = "/api/orders";

export const getAllProducts = () => {
  return apiClient.get(API_URL);
};

export const getAvailableProducts = () => {
  return apiClient.get(`${API_URL}/available`);
};

export const getProductsByCategory = (categoryId) => {
  return apiClient.get(`${API_URL}/category/${categoryId}`);
};

export const saveFeedback = (feedback) => {
  return apiClient.post(FEEDBACK_URL, feedback);
};

export const getAllFeedback = () => {
  return apiClient.get(FEEDBACK_URL);
};

export const createOrder = (order) => {
  return apiClient.post(ORDERS_URL, order);
};

export const getAllOrders = () => {
  return apiClient.get(ORDERS_URL);
};

export const updateOrderStatus = (orderId, status) => {
  return apiClient.patch(
    `${ORDERS_URL}/${orderId}/status?status=${encodeURIComponent(status)}`,
  );
};
