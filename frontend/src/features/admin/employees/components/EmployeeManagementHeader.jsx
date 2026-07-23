/*
 * Antetul paginii pentru administrarea
 * angajatilor.
 */
function EmployeeManagementHeader({ loading, onReload, onOpenCreateForm }) {
  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  return (
    <header className="admin-header">
      <h1>Administrare angajati</h1>

      <div className="admin-header-actions">
        <button
          type="button"
          className="admin-nav-button"
          onClick={onReload}
          disabled={loading}
        >
          {loading ? "Se actualizeaza..." : "Actualizeaza"}
        </button>

        <button
          type="button"
          className="admin-nav-button"
          onClick={onOpenCreateForm}
        >
          Adauga angajat
        </button>

        <button
          type="button"
          className="logout-button"
          onClick={handleBackToAdmin}
        >
          Inapoi
        </button>
      </div>
    </header>
  );
}

export default EmployeeManagementHeader;
