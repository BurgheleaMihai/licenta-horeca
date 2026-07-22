import apiClient from "./apiClient";

const EMPLOYEES_URL = "/api/employees";

export const getEmployees = async () => {
  const response = await apiClient.get(EMPLOYEES_URL);

  return response.data;
};

export const getEmployee = async (id) => {
  const response = await apiClient.get(`${EMPLOYEES_URL}/${id}`);

  return response.data;
};

export const createEmployee = async (employeeData) => {
  const response = await apiClient.post(EMPLOYEES_URL, employeeData);

  return response.data;
};

export const updateEmployee = async (id, employeeData) => {
  const response = await apiClient.put(`${EMPLOYEES_URL}/${id}`, employeeData);

  return response.data;
};

export const changeEmployeeStatus = async (id, active) => {
  const response = await apiClient.patch(
    `${EMPLOYEES_URL}/${id}/status`,
    null,
    {
      params: {
        active,
      },
    },
  );

  return response.data;
};

export const changeEmployeeRole = async (id, role) => {
  const response = await apiClient.patch(`${EMPLOYEES_URL}/${id}/role`, null, {
    params: {
      role,
    },
  });

  return response.data;
};
