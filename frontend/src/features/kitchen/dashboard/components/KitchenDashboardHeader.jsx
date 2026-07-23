function KitchenDashboardHeader({ onLogout }) {
  return (
    <header className="kitchen-header">
      <h1>Panou bucatarie</h1>

      <button type="button" className="logout-button" onClick={onLogout}>
        Logout
      </button>
    </header>
  );
}

export default KitchenDashboardHeader;
