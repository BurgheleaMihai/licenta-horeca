import axios from "axios";

const API_URL = "/api/table-sessions";

export const getActiveTableSessions = () => {
  return axios.get(`${API_URL}/active`);
};

export const createTableSessionForTable = (tableId) => {
  return axios.post(`${API_URL}/table/${tableId}`);
};

export const closeTableSession = (sessionId) => {
  return axios.put(`${API_URL}/${sessionId}/close`);
};

export const validateTableSessionCode = (sessionCode) => {
  return axios.get( `${API_URL}/code/${encodeURIComponent(sessionCode)}` );
};