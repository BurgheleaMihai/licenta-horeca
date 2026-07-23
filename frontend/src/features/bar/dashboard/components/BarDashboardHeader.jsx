function BarDashboardHeader({ onLogout }) {
  return (
    <header className="bar-header">
      <h1>Panou bar</h1>

      <button type="button" className="logout-button" onClick={onLogout}>
        Logout
      </button>
    </header>
  );
}

export default BarDashboardHeader;
