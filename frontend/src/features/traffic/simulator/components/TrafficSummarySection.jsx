function TrafficSummarySection({
  trafficSummary,
  onSimulateEntry,
  onSimulateExit,
}) {
  return (
    <section className="sensor-section">
      <h2>Status curent</h2>

      <div className="sensor-grid">
        <div className="sensor-card">
          <h3>Intrari</h3>

          <p>{trafficSummary.entries}</p>
        </div>

        <div className="sensor-card">
          <h3>Iesiri</h3>

          <p>{trafficSummary.exits}</p>
        </div>

        <div className="sensor-card">
          <h3>Ocupare estimata</h3>

          <p>{trafficSummary.estimatedOccupancy} clienti</p>
        </div>
      </div>

      <div className="sensor-actions">
        <button type="button" onClick={onSimulateEntry}>
          Simuleaza intrare
        </button>

        <button type="button" onClick={onSimulateExit}>
          Simuleaza iesire
        </button>
      </div>
    </section>
  );
}

export default TrafficSummarySection;
