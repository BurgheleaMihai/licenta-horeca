import { useEffect, useState } from "react";
import { getAllOrders } from "../api/orderApi";
import { getAllFeedback } from "../api/productApi";
import { getUnavailableAuxiliarySupplies } from "../api/auxiliarySupplyApi";
import {
  getDecisionSummary,
  getLatestUnlabeledDecisionRecord,
  labelDecisionRecord,
  retrainDecisionModels,
} from "../api/decisionApi";

function AdminPage() {
  const [allOrders, setAllOrders] = useState([]);
  const [feedbackList, setFeedbackList] = useState([]);
  const [periodFilter, setPeriodFilter] = useState("ALL");
  const [errorMessage, setErrorMessage] = useState("");
  const [unavailableSupplies, setUnavailableSupplies] = useState([]);

  const [decisionSummary, setDecisionSummary] = useState(null);
  const [decisionLoading, setDecisionLoading] = useState(false);
  const [decisionError, setDecisionError] = useState("");

  const [latestUnlabeledRecord, setLatestUnlabeledRecord] = useState(null);

  const [labelLoading, setLabelLoading] = useState(false);
  const [labelSaving, setLabelSaving] = useState(false);
  const [labelError, setLabelError] = useState("");
  const [labelMessage, setLabelMessage] = useState("");

  const [observedTrafficLevel, setObservedTrafficLevel] = useState("SCAZUT");
  const [observedDelayRisk, setObservedDelayRisk] = useState("SCAZUT");

  const [actualWaiters, setActualWaiters] = useState("0");
  const [actualKitchenStaff, setActualKitchenStaff] = useState("0");
  const [actualBarStaff, setActualBarStaff] = useState("0");

  const [retraining, setRetraining] = useState(false);
  const [retrainingMessage, setRetrainingMessage] = useState("");
  const [retrainingError, setRetrainingError] = useState("");

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  const loadAdminData = () => {
    getAllOrders()
      .then((response) => {
        setAllOrders(response.data);
      })
      .catch(() => {
        setErrorMessage("Comenzile nu au putut fi incarcate.");
      });

    getUnavailableAuxiliarySupplies()
      .then((response) => {
        setUnavailableSupplies(response.data);
      })
      .catch(() => {
        setErrorMessage("Produsele auxiliare lipsa nu au putut fi incarcate.");
      });

    getAllFeedback()
      .then((response) => {
        setFeedbackList(response.data);
      })
      .catch(() => {
        setErrorMessage("Feedback-ul nu a putut fi incarcat.");
      });
  };

  const loadLatestUnlabeledRecord = () => {
    setLabelLoading(true);
    setLabelError("");

    getLatestUnlabeledDecisionRecord()
      .then((response) => {
        setLatestUnlabeledRecord(response.data);
      })
      .catch((error) => {
        if (error.response?.status === 404) {
          setLatestUnlabeledRecord(null);
          return;
        }

        console.error("Eroare la incarcarea predictiei neetichetate:", error);
        setLabelError("Ultima predictie neetichetata nu a putut fi incarcata.");
      })
      .finally(() => {
        setLabelLoading(false);
      });
  };

  const loadDecisionSummary = () => {
    setDecisionLoading(true);
    setDecisionError("");
    setLabelMessage("");
    setLabelError("");

    getDecisionSummary()
      .then((response) => {
        setDecisionSummary(response.data);

        /*
         * Apelul pentru predictie creeaza o noua
         * inregistrare in decision_training_records.
         */
        loadLatestUnlabeledRecord();
      })
      .catch((error) => {
        console.error("Eroare la incarcarea predictiilor:", error);
        setDecisionError("Predictiile nu au putut fi incarcate. Verifica daca AI Service este pornit.");
      })
      .finally(() => {
        setDecisionLoading(false);
      });
  };

  useEffect(() => {
    loadAdminData();
    loadLatestUnlabeledRecord();
  }, []);

  const handleLabelSubmit = (event) => {
    event.preventDefault();

    if (!latestUnlabeledRecord?.id) {
      setLabelError("Nu exista nicio predictie care poate fi etichetata.");
      return;
    }

    if (actualWaiters === "" || actualKitchenStaff === "" || actualBarStaff === "") {
      setLabelError("Completeaza numarul real de angajati.");
      return;
    }

    const waiters = Number(actualWaiters);
    const kitchenStaff = Number(actualKitchenStaff);
    const barStaff = Number(actualBarStaff);

    if (
      Number.isNaN(waiters) ||
      Number.isNaN(kitchenStaff) ||
      Number.isNaN(barStaff) ||
      waiters < 0 ||
      kitchenStaff < 0 ||
      barStaff < 0
    ) {
      setLabelError("Numarul angajatilor trebuie sa fie zero sau mai mare.");
      return;
    }

    const labelData = {
      observedTrafficLevel,
      observedDelayRisk,
      actualWaiters: waiters,
      actualKitchenStaff: kitchenStaff,
      actualBarStaff: barStaff,
    };

    setLabelSaving(true);
    setLabelError("");
    setLabelMessage("");

    labelDecisionRecord(latestUnlabeledRecord.id, labelData)
      .then(() => {
        setLabelMessage(`Inregistrarea #${latestUnlabeledRecord.id} a fost etichetata cu succes.`);
        setLatestUnlabeledRecord(null);
        loadLatestUnlabeledRecord();
      })
      .catch((error) => {
        console.error("Eroare la salvarea label-urilor:", error);

        if (error.response?.status === 409) {
          setLabelError("Aceasta inregistrare a fost deja etichetata.");
          loadLatestUnlabeledRecord();
          return;
        }

        setLabelError("Rezultatele reale nu au putut fi salvate.");
      })
      .finally(() => {
        setLabelSaving(false);
      });
  };

  const handleRetrainModels = () => {
    setRetraining(true);
    setRetrainingMessage("");
    setRetrainingError("");

    retrainDecisionModels()
      .then((response) => {
        setRetrainingMessage(response.data?.message || "Reantrenarea a fost finalizata.");
      })
      .catch((error) => {
        const message = error.response?.data?.message || "Modelele nu au putut fi reantrenate.";

        /*
         * Lipsa celor 30 de inregistrari este o
         * conditie normala, nu o eroare tehnica.
         */
        if (error.response?.data?.status === "blocked") {
          setRetrainingMessage(message);
          return;
        }

        setRetrainingError(message);
      })
      .finally(() => {
        setRetraining(false);
      });
  };

  const servedOrders = allOrders.filter((order) => order.status === "SERVITA");

  const isOrderInSelectedPeriod = (order) => {
    if (periodFilter === "ALL") {
      return true;
    }

    const orderDate = new Date(order.createdAt);
    const today = new Date();

    if (periodFilter === "TODAY") {
      return orderDate.toDateString() === today.toDateString();
    }

    if (periodFilter === "LAST_7_DAYS") {
      const sevenDaysAgo = new Date();
      sevenDaysAgo.setDate(today.getDate() - 7);

      return orderDate >= sevenDaysAgo;
    }

    return true;
  };

  const filteredOrders = servedOrders.filter(isOrderInSelectedPeriod);

  const totalSales = filteredOrders.reduce((sum, order) => {
    return sum + Number(order.totalPrice);
  }, 0);

  const productSales = {};

  filteredOrders.forEach((order) => {
    order.items?.forEach((item) => {
      const productName = item.product?.name || "Produs necunoscut";

      if (!productSales[productName]) {
        productSales[productName] = 0;
      }

      productSales[productName] += Number(item.quantity);
    });
  });

  const productSalesList = Object.entries(productSales)
    .map(([name, quantity]) => ({
      name,
      quantity,
    }))
    .sort((firstProduct, secondProduct) => secondProduct.quantity - firstProduct.quantity);

  const totalProductsSold = productSalesList.reduce((sum, product) => {
    return sum + product.quantity;
  }, 0);

  const averageRating =
    feedbackList.length === 0
      ? 0
      : feedbackList.reduce((sum, feedback) => {
          return sum + Number(feedback.rating);
        }, 0) / feedbackList.length;

  return (
    <div className="admin-page">
      <header className="admin-header">
        <h1>Panou administrare</h1>

        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <section className="admin-section">
        <h2>Filtru perioada</h2>

        <select value={periodFilter} onChange={(event) => setPeriodFilter(event.target.value)}>
          <option value="ALL">Toate comenzile</option>
          <option value="TODAY">Azi</option>
          <option value="LAST_7_DAYS">Ultimele 7 zile</option>
        </select>
      </section>

      <section className="admin-section">
        <h2>Indicatori conducere</h2>

        <div className="admin-grid">
          <div className="admin-card">
            <h3>Comenzi finalizate</h3>
            <p>{filteredOrders.length}</p>
          </div>

          <div className="admin-card">
            <h3>Total vanzari</h3>
            <p>{totalSales.toFixed(2)} lei</p>
          </div>

          <div className="admin-card">
            <h3>Produse vandute</h3>
            <p>{totalProductsSold}</p>
          </div>

          <div className="admin-card">
            <h3>Rating mediu</h3>
            <p>{averageRating.toFixed(2)} / 5</p>
          </div>
        </div>
      </section>

      <section className="admin-section">
        <h2>Sistem de decizie</h2>

        <button type="button" onClick={loadDecisionSummary} disabled={decisionLoading}>
          {decisionLoading ? "Se actualizeaza..." : "Actualizeaza predictiile"}
        </button>

        {decisionError && <p className="error-message">{decisionError}</p>}

        {decisionSummary && (
          <div className="admin-grid">
            <div className="admin-card">
              <h3>Nivel trafic estimat</h3>
              <p>{decisionSummary.trafficLevel}</p>
            </div>

            <div className="admin-card">
              <h3>Ospatari recomandati</h3>
              <p>{decisionSummary.recommendedWaiters}</p>
            </div>

            <div className="admin-card">
              <h3>Personal bucatarie recomandat</h3>
              <p>{decisionSummary.recommendedKitchenStaff}</p>
            </div>

            <div className="admin-card">
              <h3>Personal bar recomandat</h3>
              <p>{decisionSummary.recommendedBarStaff}</p>
            </div>

            <div className="admin-card">
              <h3>Risc de intarziere</h3>
              <p>{decisionSummary.delayRisk}</p>
            </div>
          </div>
        )}
      </section>

      <section className="admin-section">
        <h2>Rezultate reale pentru reantrenare</h2>

        <p>
          Completeaza rezultatele observate pentru ultima predictie. Aceste valori vor fi folosite ca label-uri la
          reantrenarea modelelor.
        </p>

        {labelLoading && <p>Se cauta ultima predictie neetichetata...</p>}

        {labelError && <p className="error-message">{labelError}</p>}

        {labelMessage && <p className="feedback-message">{labelMessage}</p>}

        {!labelLoading && !latestUnlabeledRecord && (
          <p>
            Nu exista momentan nicio predictie neetichetata. Apasa „Actualizeaza predictiile” pentru a crea una noua.
          </p>
        )}

        {latestUnlabeledRecord && (
          <>
            <div className="admin-grid">
              <div className="admin-card">
                <h3>Inregistrare</h3>

                <p>ID: {latestUnlabeledRecord.id}</p>

                <p>
                  Data:{" "}
                  {latestUnlabeledRecord.createdAt
                    ? new Date(latestUnlabeledRecord.createdAt).toLocaleString()
                    : "necunoscuta"}
                </p>
              </div>

              <div className="admin-card">
                <h3>Predictie trafic</h3>
                <p>{latestUnlabeledRecord.predictedTrafficLevel}</p>
              </div>

              <div className="admin-card">
                <h3>Predictie intarziere</h3>
                <p>{latestUnlabeledRecord.predictedDelayRisk}</p>
              </div>

              <div className="admin-card">
                <h3>Stare observata</h3>

                <p>Comenzi active: {latestUnlabeledRecord.activeOrders}</p>
                <p>Mese ocupate: {latestUnlabeledRecord.occupiedTables}</p>
                <p>Comenzi recente: {latestUnlabeledRecord.ordersLast30Min}</p>
              </div>
            </div>

            <form className="decision-label-form" onSubmit={handleLabelSubmit}>
              <div className="decision-label-grid">
                <div className="filter-group">
                  <label htmlFor="observed-traffic">Trafic observat</label>

                  <select
                    id="observed-traffic"
                    value={observedTrafficLevel}
                    onChange={(event) => setObservedTrafficLevel(event.target.value)}
                  >
                    <option value="SCAZUT">Scazut</option>
                    <option value="MEDIU">Mediu</option>
                    <option value="RIDICAT">Ridicat</option>
                  </select>
                </div>

                <div className="filter-group">
                  <label htmlFor="observed-delay">Risc real de intarziere</label>

                  <select
                    id="observed-delay"
                    value={observedDelayRisk}
                    onChange={(event) => setObservedDelayRisk(event.target.value)}
                  >
                    <option value="SCAZUT">Scazut</option>
                    <option value="MEDIU">Mediu</option>
                    <option value="RIDICAT">Ridicat</option>
                  </select>
                </div>

                <div className="filter-group">
                  <label htmlFor="actual-waiters">Ospatari prezenti</label>

                  <input
                    id="actual-waiters"
                    type="number"
                    min="0"
                    value={actualWaiters}
                    onChange={(event) => setActualWaiters(event.target.value)}
                  />
                </div>

                <div className="filter-group">
                  <label htmlFor="actual-kitchen-staff">Personal bucatarie prezent</label>

                  <input
                    id="actual-kitchen-staff"
                    type="number"
                    min="0"
                    value={actualKitchenStaff}
                    onChange={(event) => setActualKitchenStaff(event.target.value)}
                  />
                </div>

                <div className="filter-group">
                  <label htmlFor="actual-bar-staff">Personal bar prezent</label>

                  <input
                    id="actual-bar-staff"
                    type="number"
                    min="0"
                    value={actualBarStaff}
                    onChange={(event) => setActualBarStaff(event.target.value)}
                  />
                </div>
              </div>

              <button type="submit" disabled={labelSaving}>
                {labelSaving ? "Se salveaza..." : "Salveaza rezultatele reale"}
              </button>
            </form>
          </>
        )}

        <div className="retraining-area">
          <h3>Reantrenare modele</h3>

          <p>Reantrenarea este permisa dupa colectarea a cel putin 30 de inregistrari etichetate.</p>

          <button type="button" onClick={handleRetrainModels} disabled={retraining}>
            {retraining ? "Se reantreneaza..." : "Reantreneaza modelele"}
          </button>

          {retrainingMessage && <p className="feedback-message">{retrainingMessage}</p>}

          {retrainingError && <p className="error-message">{retrainingError}</p>}
        </div>
      </section>

      <section className="admin-section">
        <h2>Produse auxiliare lipsa in depozit</h2>

        {unavailableSupplies.length === 0 ? (
          <p>Nu exista produse auxiliare semnalate ca lipsa.</p>
        ) : (
          <div className="admin-grid">
            {unavailableSupplies.map((supply) => (
              <div key={supply.id} className="admin-card">
                <h3>{supply.name}</h3>

                <p>Categorie: {supply.category}</p>
                <p>Status: lipsa in depozit</p>

                <p>
                  Semnalat la: {supply.reportedAt ? new Date(supply.reportedAt).toLocaleString() : "necunoscut"}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="admin-section">
        <h2>Top produse vandute</h2>

        {productSalesList.length === 0 ? (
          <p>Nu exista produse vandute in perioada selectata.</p>
        ) : (
          <div className="admin-grid">
            {productSalesList.map((product) => (
              <div key={product.name} className="admin-card">
                <h3>{product.name}</h3>
                <p>Cantitate vanduta: {product.quantity}</p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default AdminPage;