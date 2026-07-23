/*
 * Antetul paginii cu istoricul comenzilor.
 *
 * Gestioneaza navigarea spre panoul admin,
 * pagina de statistici si deconectarea.
 */
function OrderHistoryHeader() {
  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  const handleOpenStatistics = () => {
    globalThis.location.href = "/admin/statistics";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  return (
    <header className="admin-order-history-header">
      <h1>Istoric comenzi</h1>

      <p>
        Cauta comenzile servite sau anulate dupa perioada, status, masa sau
        valoare.
      </p>

      <div className="admin-header-actions">
        <button
          type="button"
          className="admin-nav-button"
          onClick={handleBackToAdmin}
        >
          Inapoi la administrare
        </button>

        <button
          type="button"
          className="admin-nav-button"
          onClick={handleOpenStatistics}
        >
          Statistici si rapoarte
        </button>

        <button type="button" className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default OrderHistoryHeader;
