import apiClient from "./apiClient";

const API_URL = "/api/table-sessions";

export const getActiveTableSessions = () => {
  return apiClient.get(`${API_URL}/active`);
};

export const createTableSessionForTable = (tableId) => {
  return apiClient.post(`${API_URL}/table/${tableId}`);
};

export const closeTableSession = (sessionId) => {
  return apiClient.put(`${API_URL}/${sessionId}/close`);
};
