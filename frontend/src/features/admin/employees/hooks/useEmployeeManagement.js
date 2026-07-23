import { useEffect, useMemo, useState } from "react";

import {
  changeEmployeeStatus,
  createEmployee,
  getEmployees,
  updateEmployee,
} from "../../../../api/employeeApi";
import {
  getEmployeesForRole,
  getFilteredEmployees,
  validateEmployeeForm,
} from "../utils/employeeManagementUtils";

/*
 * Gestioneaza datele, formularul, filtrele
 * si operatiile pentru administrarea angajatilor.
 */
function useEmployeeManagement() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [statusChangingId, setStatusChangingId] = useState(null);

  const [errorMessage, setErrorMessage] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  const [selectedRole, setSelectedRole] = useState("");

  const [selectedEmployeeId, setSelectedEmployeeId] = useState("");

  const [showEmployeeForm, setShowEmployeeForm] = useState(false);

  const [editingEmployeeId, setEditingEmployeeId] = useState(null);

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("WAITER");

  const resetForm = () => {
    setFullName("");
    setEmail("");
    setPassword("");
    setRole("WAITER");
    setEditingEmployeeId(null);
    setShowEmployeeForm(false);
  };

  const loadEmployees = () => {
    setLoading(true);
    setErrorMessage("");

    getEmployees()
      .then((employeeList) => {
        setEmployees(Array.isArray(employeeList) ? employeeList : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea angajatilor:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Angajatii nu au putut fi incarcati.",
        );
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    let componentActive = true;

    getEmployees()
      .then((employeeList) => {
        if (!componentActive) {
          return;
        }

        setEmployees(Array.isArray(employeeList) ? employeeList : []);
      })
      .catch((error) => {
        if (!componentActive) {
          return;
        }

        console.error("Eroare la incarcarea angajatilor:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Angajatii nu au putut fi incarcati.",
        );
      })
      .finally(() => {
        if (componentActive) {
          setLoading(false);
        }
      });

    return () => {
      componentActive = false;
    };
  }, []);

  const handleOpenCreateForm = () => {
    setErrorMessage("");
    setSuccessMessage("");
    setEditingEmployeeId(null);
    setFullName("");
    setEmail("");
    setPassword("");
    setRole("WAITER");
    setShowEmployeeForm(true);
  };

  const handleOpenEditForm = (employee) => {
    setErrorMessage("");
    setSuccessMessage("");
    setEditingEmployeeId(employee.id);
    setFullName(employee.fullName || "");
    setEmail(employee.email || "");
    setPassword("");
    setRole(employee.role || "WAITER");
    setShowEmployeeForm(true);
  };

  const handleEmployeeSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");
    setSuccessMessage("");

    const {
      errorMessage: validationError,
      normalizedFullName,
      normalizedEmail,
    } = validateEmployeeForm({
      fullName,
      email,
      password,
      editingEmployeeId,
    });

    if (validationError) {
      setErrorMessage(validationError);

      return;
    }

    setSaving(true);

    const request =
      editingEmployeeId === null
        ? createEmployee({
            fullName: normalizedFullName,
            email: normalizedEmail,
            password,
            role,
          })
        : updateEmployee(editingEmployeeId, {
            fullName: normalizedFullName,
            email: normalizedEmail,
            role,
          });

    request
      .then(() => {
        setSuccessMessage(
          editingEmployeeId === null
            ? "Angajatul a fost adaugat cu succes."
            : "Datele angajatului au fost actualizate.",
        );

        resetForm();
        loadEmployees();
      })
      .catch((error) => {
        console.error("Eroare la salvarea angajatului:", error);

        const backendMessage = error.response?.data?.message;

        setErrorMessage(
          backendMessage ||
            (editingEmployeeId === null
              ? "Angajatul nu a putut fi creat."
              : "Angajatul nu a putut fi actualizat."),
        );
      })
      .finally(() => {
        setSaving(false);
      });
  };

  const handleChangeStatus = (employee) => {
    const newStatus = !employee.active;

    const confirmed = globalThis.confirm(
      newStatus
        ? `Activezi angajatul ${employee.fullName}?`
        : `Dezactivezi angajatul ${employee.fullName}?`,
    );

    if (!confirmed) {
      return;
    }

    setStatusChangingId(employee.id);
    setErrorMessage("");
    setSuccessMessage("");

    changeEmployeeStatus(employee.id, newStatus)
      .then(() => {
        setSuccessMessage(
          newStatus
            ? "Angajatul a fost activat."
            : "Angajatul a fost dezactivat.",
        );

        loadEmployees();
      })
      .catch((error) => {
        console.error("Eroare la schimbarea statusului:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Statusul angajatului nu a putut fi modificat.",
        );
      })
      .finally(() => {
        setStatusChangingId(null);
      });
  };

  const employeesForSelectedRole = useMemo(
    () => getEmployeesForRole(employees, selectedRole),
    [employees, selectedRole],
  );

  const filteredEmployees = useMemo(
    () => getFilteredEmployees(employees, selectedRole, selectedEmployeeId),
    [employees, selectedRole, selectedEmployeeId],
  );

  const handleFullNameChange = (event) => {
    setFullName(event.target.value);
  };

  const handleEmailChange = (event) => {
    setEmail(event.target.value);
  };

  const handlePasswordChange = (event) => {
    setPassword(event.target.value);
  };

  const handleRoleChange = (event) => {
    setRole(event.target.value);
  };

  const handleSelectedRoleChange = (event) => {
    setSelectedRole(event.target.value);
    setSelectedEmployeeId("");
  };

  const handleSelectedEmployeeChange = (event) => {
    setSelectedEmployeeId(event.target.value);
  };

  return {
    loading,
    saving,
    statusChangingId,
    errorMessage,
    successMessage,
    selectedRole,
    selectedEmployeeId,
    showEmployeeForm,
    editingEmployeeId,
    fullName,
    email,
    password,
    role,
    employeesForSelectedRole,
    filteredEmployees,
    resetForm,
    loadEmployees,
    handleOpenCreateForm,
    handleOpenEditForm,
    handleEmployeeSubmit,
    handleChangeStatus,
    handleFullNameChange,
    handleEmailChange,
    handlePasswordChange,
    handleRoleChange,
    handleSelectedRoleChange,
    handleSelectedEmployeeChange,
  };
}

export default useEmployeeManagement;
