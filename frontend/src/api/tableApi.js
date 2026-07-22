import apiClient from "./apiClient";

const API_URL = "/api/tables";

export const getAllTables = () => {
  return apiClient.get(API_URL);
};

export const getActiveTables = () => {
  return apiClient.get(`${API_URL}/active`);
};

export const getTableById = (tableId) => {
  return apiClient.get(`${API_URL}/${tableId}`);
};
