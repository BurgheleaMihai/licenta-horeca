import axios from "axios";

const TRAFFIC_URL = "http://localhost:8080/api/traffic";

export const getTrafficSummary = () => {
  return axios.get(`${TRAFFIC_URL}/summary`);
};

export const registerEntry = () => {
  return axios.post(`${TRAFFIC_URL}/entry`);
};

export const registerExit = () => {
  return axios.post(`${TRAFFIC_URL}/exit`);
};