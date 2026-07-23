/*
 * Antetul paginii cu articolele de stoc
 * semnalate ca indisponibile.
 */
function UnavailableSuppliesHeader({ loading, onReload }) {
  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  return (
    <>
      <header className="admin-header">
        <h1>Articole lipsa</h1>

        <div className="admin-header-actions">
          <button
            type="button"
            className="admin-nav-button"
            onClick={handleBackToAdmin}
          >
            Inapoi la panou
          </button>

          <button
            type="button"
            className="logout-button"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      </header>

      <div className="decision-section-header">
        <div>
          <h2>Articole semnalate ca indisponibile</h2>

          <p>
            Lista include articole auxiliare, produse din depozit, ingrediente,
            fructe si legume care au fost raportate ca lipsa.
          </p>
        </div>

        <button
          type="button"
          className="decision-refresh-button"
          onClick={onReload}
          disabled={loading}
        >
          {loading ? "Se actualizeaza..." : "Actualizeaza lista"}
        </button>
      </div>
    </>
  );
}

export default UnavailableSuppliesHeader;
