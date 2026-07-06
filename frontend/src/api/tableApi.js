import axios from "axios";

const API_URL = "/api/tables";

export const getAllTables = () => {
  return axios.get(API_URL);
};

export const getActiveTables = () => {
  return axios.get(`${API_URL}/active`);
};

export const getTableById = (tableId) => {
  return axios.get(`${API_URL}/${tableId}`);
};