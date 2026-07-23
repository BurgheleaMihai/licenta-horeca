/*
 * Afiseaza distributia comenzilor active
 * pe principalele zone operationale.
 */
function ManagerOperationalStatusSection({
  newOrdersCount,
  inPreparationOrdersCount,
  readyOrdersCount,
}) {
  return (
    <section className="manager-section">
      <h2>Status operational pe zone</h2>

      <div className="manager-grid">
        <div className="manager-card">
          <h3>Ospatar - comenzi noi</h3>

          <p>{newOrdersCount}</p>
        </div>

        <div className="manager-card">
          <h3>Bucatarie/Bar - in preparare</h3>

          <p>{inPreparationOrdersCount}</p>
        </div>

        <div className="manager-card">
          <h3>Ospatar - comenzi gata</h3>

          <p>{readyOrdersCount}</p>
        </div>
      </div>
    </section>
  );
}

export default ManagerOperationalStatusSection;
