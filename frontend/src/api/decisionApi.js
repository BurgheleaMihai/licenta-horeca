import axios from "axios";

const API_URL = "http://localhost:8080/api/decision";

export const getDecisionSummary = () => {
  return axios.get(`${API_URL}/summary`);
};