import axios from "axios";

const AUTH_URL = "/api/auth";

export const login = (credentials) => {
  return axios.post(`${AUTH_URL}/login`, credentials);
};