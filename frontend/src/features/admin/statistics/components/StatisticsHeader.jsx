/*
 * Antetul paginii de statistici.
 *
 * Gestioneaza navigarea inapoi la panoul
 * administratorului si deconectarea.
 */
function StatisticsHeader() {
  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  return (
    <header className="admin-statistics-header">
      <h1>Statistici si rapoarte</h1>

      <p>Analiza comenzilor, vanzarilor si feedback-ului pe perioade.</p>

      <div className="admin-header-actions">
        <button
          type="button"
          className="admin-nav-button"
          onClick={handleBackToAdmin}
        >
          Inapoi la administrare
        </button>

        <button type="button" className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}

export default StatisticsHeader;
