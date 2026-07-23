/*
 * Valideaza datele introduse in formularul
 * pentru crearea sau modificarea unui angajat.
 */
export const validateEmployeeForm = ({
  fullName,
  email,
  password,
  editingEmployeeId,
}) => {
  const normalizedFullName = fullName.trim();
  const normalizedEmail = email.trim();

  if (!normalizedFullName) {
    return {
      errorMessage: "Numele angajatului este obligatoriu.",
      normalizedFullName,
      normalizedEmail,
    };
  }

  if (!normalizedEmail) {
    return {
      errorMessage: "Emailul angajatului este obligatoriu.",
      normalizedFullName,
      normalizedEmail,
    };
  }

  if (editingEmployeeId === null && password.length < 8) {
    return {
      errorMessage: "Parola trebuie sa aiba cel putin 8 caractere.",
      normalizedFullName,
      normalizedEmail,
    };
  }

  return {
    errorMessage: "",
    normalizedFullName,
    normalizedEmail,
  };
};

/*
 * Returneaza angajatii care apartin
 * rolului selectat.
 */
export const getEmployeesForRole = (employees, selectedRole) => {
  if (!selectedRole) {
    return [];
  }

  return employees.filter((employee) => employee.role === selectedRole);
};

/*
 * Filtreaza lista finala dupa rol si,
 * optional, dupa angajatul selectat.
 */
export const getFilteredEmployees = (
  employees,
  selectedRole,
  selectedEmployeeId,
) => {
  if (!selectedRole) {
    return employees;
  }

  const employeesForRole = getEmployeesForRole(employees, selectedRole);

  if (!selectedEmployeeId) {
    return employeesForRole;
  }

  return employeesForRole.filter(
    (employee) => String(employee.id) === selectedEmployeeId,
  );
};
