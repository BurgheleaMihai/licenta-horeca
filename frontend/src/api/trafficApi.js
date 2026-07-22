import apiClient from "./apiClient";

const TRAFFIC_URL = "/api/traffic";

export const getTrafficSummary = () => {
  return apiClient.get(`${TRAFFIC_URL}/summary`);
};

export const registerEntry = () => {
  return apiClient.post(`${TRAFFIC_URL}/entry`);
};

export const registerExit = () => {
  return apiClient.post(`${TRAFFIC_URL}/exit`);
};
