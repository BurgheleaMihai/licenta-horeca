import { useEffect, useMemo, useState } from "react";
import {
  changeEmployeeStatus,
  createEmployee,
  getEmployees,
  updateEmployee,
} from "../api/employeeApi";

const roleLabels = {
  WAITER: "Ospatar",
  KITCHEN: "Bucatarie",
  BAR: "Bar",
  MANAGER: "Manager",
  ADMIN: "Administrator",
};

const employeeActionStyles = {
  container: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "10px",
    flexWrap: "wrap",
    minWidth: "230px",
  },
  editButton: {
    minWidth: "96px",
    padding: "9px 14px",
    border: "1px solid #cbd5e1",
    borderRadius: "8px",
    backgroundColor: "#f8fafc",
    color: "#1e293b",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  disableButton: {
    minWidth: "112px",
    padding: "9px 14px",
    border: "1px solid #fecaca",
    borderRadius: "8px",
    backgroundColor: "#fee2e2",
    color: "#b91c1c",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  enableButton: {
    minWidth: "112px",
    padding: "9px 14px",
    border: "1px solid #bbf7d0",
    borderRadius: "8px",
    backgroundColor: "#dcfce7",
    color: "#15803d",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
  },
  disabledButton: {
    opacity: 0.6,
    cursor: "not-allowed",
  },
};

const employeeFormActionStyles = {
  container: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "12px",
    flexWrap: "wrap",
    marginTop: "18px",
  },
  button: {
    width: "200px",
    minHeight: "46px",
    padding: "10px 16px",
    borderRadius: "9px",
    fontSize: "15px",
    fontWeight: "700",
    cursor: "pointer",
  },
  saveButton: {
    border: "1px solid #222222",
    backgroundColor: "#222222",
    color: "#ffffff",
  },
  cancelButton: {
    border: "1px solid #cbd5e1",
    backgroundColor: "#f8fafc",
    color: "#1e293b",
  },
  disabledButton: {
    opacity: 0.6,
    cursor: "not-allowed",
  },
};

function EmployeeManagementPage() {
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

  const handleBack = () => {
    globalThis.location.href = "/admin";
  };

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
    loadEmployees();
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

    const normalizedFullName = fullName.trim();
    const normalizedEmail = email.trim();

    if (!normalizedFullName) {
      setErrorMessage("Numele angajatului este obligatoriu.");
      return;
    }

    if (!normalizedEmail) {
      setErrorMessage("Emailul angajatului este obligatoriu.");
      return;
    }

    if (editingEmployeeId === null && password.length < 8) {
      setErrorMessage("Parola trebuie sa aiba cel putin 8 caractere.");
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

  const employeesForSelectedRole = useMemo(() => {
    if (!selectedRole) {
      return [];
    }

    return employees.filter((employee) => employee.role === selectedRole);
  }, [employees, selectedRole]);

  const filteredEmployees = useMemo(() => {
    if (!selectedRole) {
      return employees;
    }

    if (!selectedEmployeeId) {
      return employeesForSelectedRole;
    }

    return employeesForSelectedRole.filter(
      (employee) => String(employee.id) === selectedEmployeeId,
    );
  }, [employees, selectedRole, selectedEmployeeId, employeesForSelectedRole]);

  return (
    <div className="admin-page">
      <header className="admin-header">
        <h1>Administrare angajati</h1>

        <div className="admin-header-actions">
          <button
            type="button"
            className="admin-nav-button"
            onClick={loadEmployees}
            disabled={loading}
          >
            {loading ? "Se actualizeaza..." : "Actualizeaza"}
          </button>

          <button
            type="button"
            className="admin-nav-button"
            onClick={handleOpenCreateForm}
          >
            Adauga angajat
          </button>

          <button type="button" className="logout-button" onClick={handleBack}>
            Inapoi
          </button>
        </div>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      {successMessage && <p className="feedback-message">{successMessage}</p>}

      {showEmployeeForm && (
        <section className="admin-section">
          <h2>
            {editingEmployeeId === null ? "Adauga angajat" : "Editeaza angajat"}
          </h2>

          <form className="decision-label-form" onSubmit={handleEmployeeSubmit}>
            <div className="decision-label-grid">
              <div className="filter-group">
                <label htmlFor="employee-name">Nume complet</label>

                <input
                  id="employee-name"
                  type="text"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  required
                />
              </div>

              <div className="filter-group">
                <label htmlFor="employee-email">Email</label>

                <input
                  id="employee-email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </div>

              {editingEmployeeId === null && (
                <div className="filter-group">
                  <label htmlFor="employee-password">Parola</label>

                  <input
                    id="employee-password"
                    type="password"
                    minLength="8"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    required
                  />
                </div>
              )}

              <div className="filter-group">
                <label htmlFor="employee-role">Rol</label>

                <select
                  id="employee-role"
                  value={role}
                  onChange={(event) => setRole(event.target.value)}
                >
                  <option value="WAITER">Ospatar</option>

                  <option value="KITCHEN">Bucatarie</option>

                  <option value="BAR">Bar</option>

                  <option value="MANAGER">Manager</option>

                  <option value="ADMIN">Administrator</option>
                </select>
              </div>
            </div>

            <div style={employeeFormActionStyles.container}>
              <button
                type="submit"
                disabled={saving}
                style={{
                  ...employeeFormActionStyles.button,
                  ...employeeFormActionStyles.saveButton,
                  ...(saving ? employeeFormActionStyles.disabledButton : {}),
                }}
              >
                {saving
                  ? "Se salveaza..."
                  : editingEmployeeId === null
                    ? "Salveaza angajatul"
                    : "Salveaza modificarile"}
              </button>

              <button
                type="button"
                onClick={resetForm}
                disabled={saving}
                style={{
                  ...employeeFormActionStyles.button,
                  ...employeeFormActionStyles.cancelButton,
                  ...(saving ? employeeFormActionStyles.disabledButton : {}),
                }}
              >
                Anuleaza
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="admin-section">
        <h2>Lista angajatilor</h2>

        <div className="decision-label-grid">
          <div className="filter-group">
            <label htmlFor="role-filter">Rol</label>

            <select
              id="role-filter"
              value={selectedRole}
              onChange={(event) => {
                setSelectedRole(event.target.value);
                setSelectedEmployeeId("");
              }}
            >
              <option value="">Toti angajatii</option>

              <option value="WAITER">Ospatar</option>

              <option value="KITCHEN">Bucatarie</option>

              <option value="BAR">Bar</option>

              <option value="MANAGER">Manager</option>

              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="employee-filter">Angajat</label>

            <select
              id="employee-filter"
              value={selectedEmployeeId}
              onChange={(event) => setSelectedEmployeeId(event.target.value)}
              disabled={!selectedRole}
            >
              <option value="">
                {selectedRole
                  ? "Toti angajatii cu acest rol"
                  : "Selecteaza mai intai rolul"}
              </option>

              {employeesForSelectedRole.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName}
                </option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <p>Se incarca angajatii...</p>
        ) : filteredEmployees.length === 0 ? (
          <p>Nu exista angajati care sa corespunda selectiei.</p>
        ) : (
          <div className="table-wrapper">
            <table className="statistics-table">
              <thead>
                <tr>
                  <th>Nume</th>
                  <th>Email</th>
                  <th>Rol</th>
                  <th>Status</th>
                  <th>Actiuni</th>
                </tr>
              </thead>

              <tbody>
                {filteredEmployees.map((employee) => (
                  <tr key={employee.id}>
                    <td>{employee.fullName}</td>

                    <td>{employee.email}</td>

                    <td>{roleLabels[employee.role] || employee.role}</td>

                    <td>{employee.active ? "Activ" : "Inactiv"}</td>

                    <td
                      style={{
                        textAlign: "center",
                        verticalAlign: "middle",
                      }}
                    >
                      <div style={employeeActionStyles.container}>
                        <button
                          type="button"
                          style={employeeActionStyles.editButton}
                          onClick={() => handleOpenEditForm(employee)}
                        >
                          Editare
                        </button>

                        <button
                          type="button"
                          style={{
                            ...(employee.active
                              ? employeeActionStyles.disableButton
                              : employeeActionStyles.enableButton),
                            ...(statusChangingId === employee.id
                              ? employeeActionStyles.disabledButton
                              : {}),
                          }}
                          onClick={() => handleChangeStatus(employee)}
                          disabled={statusChangingId === employee.id}
                        >
                          {statusChangingId === employee.id
                            ? "Se modifica..."
                            : employee.active
                              ? "Dezactiveaza"
                              : "Activeaza"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default EmployeeManagementPage;
