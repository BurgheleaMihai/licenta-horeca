/*
 * Afiseaza datele senzorilor de intrare
 * si iesire din restaurant.
 */
function ManagerTrafficSection({ trafficSummary }) {
  return (
    <section className="manager-section">
      <h2>Senzori intrare-iesire</h2>

      <div className="manager-grid">
        <div className="manager-card">
          <h3>Intrari</h3>

          <p>{trafficSummary.entries}</p>
        </div>

        <div className="manager-card">
          <h3>Iesiri</h3>

          <p>{trafficSummary.exits}</p>
        </div>

        <div className="manager-card">
          <h3>Ocupare estimata</h3>

          <p>{trafficSummary.estimatedOccupancy} mese</p>
        </div>
      </div>
    </section>
  );
}

export default ManagerTrafficSection;
