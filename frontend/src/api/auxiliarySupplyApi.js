import axios from "axios";

const API_URL = "/api/auxiliary-supplies";

export const getAllAuxiliarySupplies = () => {
  return axios.get(API_URL);
};

export const getUnavailableAuxiliarySupplies = () => {
  return axios.get(`${API_URL}/unavailable`);
};

export const markSupplyUnavailable = (supplyId) => {
  return axios.put(`${API_URL}/${supplyId}/mark-unavailable`);
};

export const markSupplyAvailable = (supplyId) => {
  return axios.put(`${API_URL}/${supplyId}/mark-available`);
};