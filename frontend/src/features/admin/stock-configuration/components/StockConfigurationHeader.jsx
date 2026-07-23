/*
 * Antetul paginii pentru configurarea stocurilor.
 *
 * Gestioneaza revenirea la panoul principal
 * al administratorului.
 */
function StockConfigurationHeader() {
  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  return (
    <header className="stock-configuration-header">
      <h1>Configurare stocuri</h1>

      <p>Configureaza produsele de baza si variantele acestora.</p>

      <button type="button" onClick={handleBackToAdmin}>
        Inapoi la panoul de administrare
      </button>
    </header>
  );
}

export default StockConfigurationHeader;
