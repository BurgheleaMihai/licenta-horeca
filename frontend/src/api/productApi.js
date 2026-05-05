import axios from "axios";

const API_URL = "http://localhost:8080/api/products";

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
  return axios.post("http://localhost:8080/api/feedback", feedback);
};