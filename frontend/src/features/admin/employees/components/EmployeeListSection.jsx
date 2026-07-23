import {
  employeeActionStyles,
  roleLabels,
} from "../constants/employeeManagementConstants";

/*
 * Afiseaza filtrele si lista angajatilor,
 * impreuna cu actiunile disponibile.
 */
function EmployeeListSection({
  loading,
  selectedRole,
  selectedEmployeeId,
  employeesForSelectedRole,
  filteredEmployees,
  statusChangingId,
  onRoleChange,
  onEmployeeChange,
  onEdit,
  onChangeStatus,
}) {
  return (
    <section className="admin-section">
      <h2>Lista angajatilor</h2>

      <div className="decision-label-grid">
        <div className="filter-group">
          <label htmlFor="role-filter">Rol</label>

          <select id="role-filter" value={selectedRole} onChange={onRoleChange}>
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
            onChange={onEmployeeChange}
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
                        onClick={() => onEdit(employee)}
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
                        onClick={() => onChangeStatus(employee)}
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
  );
}

export default EmployeeListSection;
