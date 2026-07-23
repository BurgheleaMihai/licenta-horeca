import { employeeFormActionStyles } from "../constants/employeeManagementConstants";

/*
 * Formularul folosit pentru crearea si
 * modificarea unui angajat.
 */
function EmployeeFormSection({
  editingEmployeeId,
  fullName,
  email,
  password,
  role,
  saving,
  onFullNameChange,
  onEmailChange,
  onPasswordChange,
  onRoleChange,
  onSubmit,
  onCancel,
}) {
  return (
    <section className="admin-section">
      <h2>
        {editingEmployeeId === null ? "Adauga angajat" : "Editeaza angajat"}
      </h2>

      <form className="decision-label-form" onSubmit={onSubmit}>
        <div className="decision-label-grid">
          <div className="filter-group">
            <label htmlFor="employee-name">Nume complet</label>

            <input
              id="employee-name"
              type="text"
              value={fullName}
              onChange={onFullNameChange}
              required
            />
          </div>

          <div className="filter-group">
            <label htmlFor="employee-email">Email</label>

            <input
              id="employee-email"
              type="email"
              value={email}
              onChange={onEmailChange}
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
                onChange={onPasswordChange}
                required
              />
            </div>
          )}

          <div className="filter-group">
            <label htmlFor="employee-role">Rol</label>

            <select id="employee-role" value={role} onChange={onRoleChange}>
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
            onClick={onCancel}
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
  );
}

export default EmployeeFormSection;
