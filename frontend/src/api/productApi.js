import axios from "axios";

const API_URL = "http://localhost:8080/api/products";
const FEEDBACK_URL = "http://localhost:8080/api/feedback";
const ORDERS_URL = "http://localhost:8080/api/orders";

export const getAllProducts = () => {
  return axios.get(API_URL);
};

export const getAvailableProducts = () => {
  return axios.get(`${API_URL}/available`);
};

export const getProductsByCategory = (categoryId) => {
  return axios.get(`${API_URL}/category/${categoryId}`);
};

export const saveFeedback = (feedback) => {
  return axios.post(FEEDBACK_URL, feedback);
};

export const createOrder = (order) => {
  return axios.post(ORDERS_URL, order);
};

export const getAllOrders = () => {
  return axios.get(ORDERS_URL);
};

export const updateOrderStatus = (orderId, status) => {
  return axios.patch(`${ORDERS_URL}/${orderId}/status?status=${status}`);
};