/*
 * Antetul dashboard-ului managerului.
 */
function ManagerDashboardHeader() {
  const handleOpenStocks = () => {
    globalThis.location.href = "/manager-supplies";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  return (
    <header className="manager-header">
      <h1>Panou manager</h1>

      <div className="manager-header-actions">
        <button
          type="button"
          className="manager-nav-button"
          onClick={handleOpenStocks}
        >
          Stocuri
        </button>

        <button type="button" className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default ManagerDashboardHeader;
