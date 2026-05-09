import { useEffect, useState } from "react";
import { getTrafficSummary, registerEntry, registerExit } from "../api/trafficApi";

function SensorSimulatorPage() {
  const [trafficSummary, setTrafficSummary] = useState({
    entries: 0,
    exits: 0,
    estimatedOccupancy: 0
  });

  useEffect(() => {
    loadTrafficSummary();
  }, []);

  const loadTrafficSummary = () => {
    getTrafficSummary()
      .then((response) => setTrafficSummary(response.data))
      .catch((error) => console.error("Eroare senzori:", error));
  };

  const simulateEntry = () => {
    registerEntry().then(() => loadTrafficSummary());
  };

  const simulateExit = () => {
    registerExit().then(() => loadTrafficSummary());
  };

  return (
    <div className="sensor-page">
      <header className="sensor-header">
        <h1>Simulator senzori</h1>
        <p>Pagina folosita pentru simularea evenimentelor de intrare si iesire.</p>
      </header>

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
          <button onClick={simulateEntry}>Simuleaza intrare</button>
          <button onClick={simulateExit}>Simuleaza iesire</button>
        </div>
      </section>
    </div>
  );
}

export default SensorSimulatorPage;