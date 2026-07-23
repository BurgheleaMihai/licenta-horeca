function WaiterDashboardHeader({ onOpenQrPage, onLogout }) {
  return (
    <header className="waiter-header">
      <h1>Panou ospatar</h1>

      <div className="waiter-header-actions">
        <button
          type="button"
          className="waiter-header-button"
          onClick={onOpenQrPage}
        >
          Afiseaza QR meniu
        </button>

        <button
          type="button"
          className="waiter-header-button"
          onClick={onLogout}
        >
          Logout
        </button>
      </div>
    </header>
  );
}

export default WaiterDashboardHeader;
