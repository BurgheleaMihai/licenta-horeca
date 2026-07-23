import { clearStoredDecisionData } from "../utils/decisionStorage";

/*
 * Antetul panoului principal de administrare.
 *
 * Gestioneaza navigarea intre paginile administratorului
 * si deconectarea utilizatorului.
 */
function AdminDashboardHeader() {
  const handleOpenStatistics = () => {
    globalThis.location.href = "/admin/statistics";
  };

  const handleOpenOrderHistory = () => {
    globalThis.location.href = "/admin/order-history";
  };

  const handleOpenStockConfiguration = () => {
    globalThis.location.href = "/admin/stock-configuration";
  };

  const handleOpenUnavailableSupplies = () => {
    globalThis.location.href = "/admin/unavailable-supplies";
  };

  const handleOpenEmployees = () => {
    globalThis.location.href = "/admin/employees";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    clearStoredDecisionData();
    globalThis.location.href = "/login";
  };

  return (
    <header className="admin-header">
      <h1>Panou administrare</h1>

      <div className="admin-header-actions">
        <button
          type="button"
          className="admin-nav-button"
          onClick={handleOpenStatistics}
        >
          Statistici si rapoarte
        </button>

        <button
          type="button"
          className="admin-nav-button"
          onClick={handleOpenOrderHistory}
        >
          Istoric comenzi
        </button>

        <button
          type="button"
          className="admin-nav-button"
          onClick={handleOpenStockConfiguration}
        >
          Configurare stocuri
        </button>

        <button
          type="button"
          className="admin-nav-button"
          onClick={handleOpenUnavailableSupplies}
        >
          Articole lipsa
        </button>

        <button
          type="button"
          className="admin-nav-button"
          onClick={handleOpenEmployees}
        >
          Administrare angajati
        </button>

        <button type="button" className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default AdminDashboardHeader;
