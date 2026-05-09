import axios from "axios";

const AUTH_URL = "http://localhost:8080/api/auth";

export const login = (credentials) => {
    return axios.post(`${AUTH_URL}/login`, credentials);
};