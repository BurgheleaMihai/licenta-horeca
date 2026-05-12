import { useEffect, useState } from "react";
import {
  getAllAuxiliarySupplies,
  markSupplyUnavailable,
  markSupplyAvailable
} from "../api/auxiliarySupplyApi";

function ManagerSuppliesPage() {
  const [supplies, setSupplies] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadSupplies();
  }, []);

  const loadSupplies = () => {
    getAllAuxiliarySupplies()
      .then((response) => setSupplies(response.data))
      .catch(() => setErrorMessage("Produsele auxiliare nu au putut fi incarcate."));
  };

  const handleMarkUnavailable = (supplyId) => {
    markSupplyUnavailable(supplyId).then(() => loadSupplies());
  };

  const handleMarkAvailable = (supplyId) => {
    markSupplyAvailable(supplyId).then(() => loadSupplies());
  };

  return (
    <div className="manager-page">
      <header className="manager-header">
        <h1>Stoc auxiliar</h1>
        <button onClick={() => window.location.href = "/manager"}>
          Inapoi la panou manager
        </button>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <section className="manager-section">
        <h2>Lista produse auxiliare</h2>

        <div className="manager-grid">
          {supplies.map((supply) => (
            <div key={supply.id} className="manager-card">
              <h3>{supply.name}</h3>
              <p>Categorie: {supply.category}</p>
              <p>
                Status:{" "}
                <strong>
                  {supply.availableInWarehouse
                    ? "Disponibil in depozit"
                    : "Lipsa in depozit"}
                </strong>
              </p>

              {supply.availableInWarehouse ? (
                <button onClick={() => handleMarkUnavailable(supply.id)}>
                  Semnaleaza lipsa
                </button>
              ) : (
                <button onClick={() => handleMarkAvailable(supply.id)}>
                  Marcheaza disponibil
                </button>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

export default ManagerSuppliesPage;