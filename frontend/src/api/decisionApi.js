import apiClient from "./apiClient";

const API_URL = "/api/decision";

export const getDecisionSummary = () => {
  return apiClient.get(`${API_URL}/summary`);
};

export const getLatestUnlabeledDecisionRecord = () => {
  return apiClient.get(`${API_URL}/training-records/latest-unlabeled`);
};

export const labelDecisionRecord = (recordId, labelData) => {
  return apiClient.put(
    `${API_URL}/training-records/${recordId}/label`,
    labelData,
  );
};

export const retrainDecisionModels = () => {
  return apiClient.post(`${API_URL}/retrain`);
};
