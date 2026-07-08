import axios from "axios";

const API_URL = "/api/decision";

export const getDecisionSummary = () => {
  return axios.get(`${API_URL}/summary`);
};

export const getLatestUnlabeledDecisionRecord = () => {
  return axios.get(`${API_URL}/training-records/latest-unlabeled`);
};

export const labelDecisionRecord = (recordId, labelData) => {
  return axios.put(`${API_URL}/training-records/${recordId}/label`,labelData);
};

export const retrainDecisionModels = () => {
  return axios.post(`${API_URL}/retrain`);
};