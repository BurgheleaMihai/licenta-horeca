import apiClient from "./apiClient";

const AUTH_URL = "/api/auth";

export const login = (credentials) => {
  return apiClient.post(`${AUTH_URL}/login`, credentials);
};
