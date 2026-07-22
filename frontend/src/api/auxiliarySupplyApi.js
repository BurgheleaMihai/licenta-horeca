import apiClient from "./apiClient";

const API_URL = "/api/auxiliary-supplies";

export const getAllAuxiliarySupplies = () => {
  return apiClient.get(API_URL);
};

export const getAllActiveAuxiliarySupplies = () => {
  return apiClient.get(`${API_URL}/active`);
};

export const getAuxiliarySupplyById = (supplyId) => {
  return apiClient.get(`${API_URL}/${supplyId}`);
};

export const getUnavailableAuxiliarySupplies = () => {
  return apiClient.get(`${API_URL}/unavailable`);
};

export const createAuxiliarySupply = (supplyData) => {
  return apiClient.post(API_URL, supplyData);
};

export const updateAuxiliarySupply = (supplyId, supplyData) => {
  return apiClient.put(`${API_URL}/${supplyId}`, supplyData);
};

export const deleteAuxiliarySupply = (supplyId) => {
  return apiClient.delete(`${API_URL}/${supplyId}`);
};

export const markSupplyUnavailable = (supplyId) => {
  return apiClient.put(`${API_URL}/${supplyId}/mark-unavailable`);
};

export const markSupplyAvailable = (supplyId) => {
  return apiClient.put(`${API_URL}/${supplyId}/mark-available`);
};

export const getStockEntries = (supplyId) => {
  return apiClient.get(`${API_URL}/${supplyId}/entries`);
};

export const addStockEntry = (supplyId, entryData) => {
  return apiClient.post(`${API_URL}/${supplyId}/entries`, entryData);
};

export const updateStockEntry = (entryId, entryData) => {
  return apiClient.put(`${API_URL}/entries/${entryId}`, entryData);
};

export const deleteStockEntry = (entryId) => {
  return apiClient.delete(`${API_URL}/entries/${entryId}`);
};
