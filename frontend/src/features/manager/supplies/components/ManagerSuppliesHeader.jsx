function ManagerSuppliesHeader({ onBackToManager }) {
  return (
    <header className="manager-header">
      <h1>Stocuri restaurant</h1>

      <button type="button" onClick={onBackToManager}>
        Inapoi la panoul managerului
      </button>
    </header>
  );
}

export default ManagerSuppliesHeader;
