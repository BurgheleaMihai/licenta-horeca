import axios from "axios";

const API_URL = "http://localhost:8080/api/orders";

export const getAllOrders = () => {
  return axios.get(API_URL);
};

export const getActiveOrders = () => {
  return axios.get(`${API_URL}/active`);
};

export const updateOrderStatus = (orderId, status) => {
  return axios.put(`${API_URL}/${orderId}/status`, {
    status: status
  });
};

export const createOrder = (order) => {
  return axios.post(API_URL, order);
};

export const getKitchenOrders = () => {
  return axios.get(`${API_URL}/kitchen`);
};

export const getBarOrders = () => {
  return axios.get(`${API_URL}/bar`);
};

export const updateOrderItemStatus = (itemId, status) => {
  return axios.put(`${API_URL}/items/${itemId}/status`, {
    status: status
  });
};