import axios from "axios";

const API_URL = "http://localhost:8080/api/table-sessions";

export const getActiveTableSessions = () => {
  return axios.get(`${API_URL}/active`);
};

export const createTableSessionForTable = (tableId) => {
  return axios.post(`${API_URL}/table/${tableId}`);
};