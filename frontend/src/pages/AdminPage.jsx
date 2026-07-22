import { useEffect, useRef, useState } from "react";
import { getUnavailableAuxiliarySupplies } from "../api/auxiliarySupplyApi";
import {
  getDecisionSummary,
  getLatestUnlabeledDecisionRecord,
  labelDecisionRecord,
  retrainDecisionModels,
} from "../api/decisionApi";

const categoryLabels = {
  PACKAGING: "Ambalaje",
  CONSUMABLE: "Consumabile",
  BEVERAGE_INGREDIENT: "Ingrediente pentru bauturi",
  MEAT: "Carne",
  DAIRY: "Lactate",
  DRY_PRODUCT: "Produse uscate",
  FROZEN_PRODUCT: "Produse congelate",
  SAUCE: "Sosuri",
  OTHER_INGREDIENT: "Alte ingrediente",
  FRUIT: "Fructe",
  VEGETABLE: "Legume",
  OTHER: "Altele",
};

const stockTypeLabels = {
  AUXILIARY: "Auxiliar",
  WAREHOUSE: "Depozit",
  FRUIT_AND_VEGETABLE: "Fructe si legume",
};

const unitLabels = {
  PIECE: "buc",
  GRAM: "g",
  KILOGRAM: "kg",
  MILLILITER: "ml",
  LITER: "l",
};

const DECISION_SUMMARY_STORAGE_KEY = "adminDecisionSummary";
const DECISION_UPDATED_AT_STORAGE_KEY = "adminDecisionUpdatedAt";

const readStoredDecisionSummary = () => {
  try {
    const storedValue = sessionStorage.getItem(
      DECISION_SUMMARY_STORAGE_KEY,
    );

    return storedValue ? JSON.parse(storedValue) : null;
  } catch (error) {
    console.warn("Predictia salvata local nu a putut fi citita:", error);

    sessionStorage.removeItem(DECISION_SUMMARY_STORAGE_KEY);

    return null;
  }
};

const readStoredDecisionUpdatedAt = () => {
  const storedValue = sessionStorage.getItem(
    DECISION_UPDATED_AT_STORAGE_KEY,
  );

  if (!storedValue) {
    return null;
  }

  const storedDate = new Date(storedValue);

  if (Number.isNaN(storedDate.getTime())) {
    sessionStorage.removeItem(DECISION_UPDATED_AT_STORAGE_KEY);

    return null;
  }

  return storedDate;
};

const staffingRoles = [
  {
    key: "waiters",
    label: "Ospatari",
    activeField: "activeWaiters",
    recommendedField: "recommendedWaiters",
    deficitField: "waiterDeficit",
  },
  {
    key: "kitchen",
    label: "Bucatarie",
    activeField: "activeKitchenStaff",
    recommendedField: "recommendedKitchenStaff",
    deficitField: "kitchenDeficit",
  },
  {
    key: "bar",
    label: "Bar",
    activeField: "activeBarStaff",
    recommendedField: "recommendedBarStaff",
    deficitField: "barDeficit",
  },
];

const getLevelClassName = (level) => {
  if (level === "RIDICAT") {
    return "high";
  }

  if (level === "MEDIU") {
    return "medium";
  }

  if (level === "SCAZUT") {
    return "low";
  }

  return "unknown";
};

const getStaffingStatus = (deficit) => {
  if (deficit > 0) {
    return {
      label: `Deficit ${deficit}`,
      className: "deficit",
    };
  }

  if (deficit < 0) {
    return {
      label: `Surplus ${Math.abs(deficit)}`,
      className: "surplus",
    };
  }

  return {
    label: "Suficient",
    className: "sufficient",
  };
};

const buildOperationalRecommendations = (decisionSummary) => {
  const recommendations = [];

  if (decisionSummary.delayRisk === "RIDICAT") {
    recommendations.push({
      type: "urgent",
      text:
        "Riscul de intarziere este ridicat. " +
        "Prioritizeaza comenzile vechi si verifica " +
        "incarcarea din bucatarie si bar.",
    });
  }

  if (decisionSummary.trafficLevel === "RIDICAT") {
    recommendations.push({
      type: "warning",
      text:
        "Este estimat un varf de trafic. " +
        "Pregateste sectiile pentru un volum mai mare " +
        "de comenzi.",
    });
  }

  const roleRecommendations = [
    {
      label: "ospatari",
      deficit: decisionSummary.waiterDeficit,
      addMessage: (value) =>
        `Se recomanda suplimentarea turei cu ${value} ` +
        `${value === 1 ? "ospatar" : "ospatari"}.`,
      surplusMessage: (value) =>
        `Exista ${value} ${
          value === 1 ? "ospatar suplimentar" : "ospatari suplimentari"
        } care pot ajuta la alte activitati.`,
    },
    {
      label: "bucatarie",
      deficit: decisionSummary.kitchenDeficit,
      addMessage: (value) =>
        `Se recomanda suplimentarea bucatariei cu ${value} ` +
        `${value === 1 ? "angajat" : "angajati"}.`,
      surplusMessage: (value) =>
        `Bucataria are ${value} ${
          value === 1 ? "angajat suplimentar" : "angajati suplimentari"
        } fata de necesarul estimat.`,
    },
    {
      label: "bar",
      deficit: decisionSummary.barDeficit,
      addMessage: (value) =>
        `Se recomanda suplimentarea barului cu ${value} ` +
        `${value === 1 ? "angajat" : "angajati"}.`,
      surplusMessage: (value) =>
        `Barul are ${value} ${
          value === 1 ? "angajat suplimentar" : "angajati suplimentari"
        } care poate fi redistribuit.`,
    },
  ];

  roleRecommendations.forEach((role) => {
    if (role.deficit > 0) {
      recommendations.push({
        type: "warning",
        text: role.addMessage(role.deficit),
      });
    } else if (role.deficit < 0) {
      recommendations.push({
        type: "info",
        text: role.surplusMessage(Math.abs(role.deficit)),
      });
    }
  });

  const hasStaffDeficit = roleRecommendations.some((role) => role.deficit > 0);

  const hasStaffSurplus = roleRecommendations.some((role) => role.deficit < 0);

  if (!hasStaffDeficit && !hasStaffSurplus) {
    recommendations.push({
      type: "success",
      text:
        "Personalul activ corespunde necesarului " +
        "estimat pentru toate rolurile.",
    });
  }

  return recommendations;
};

const NumericStepper = ({
  id,
  label,
  value,
  onChange,
}) => {
  const numericValue = Number(value) || 0;

  const currentValueRef = useRef(numericValue);
  const holdTimeoutRef = useRef(null);
  const holdIntervalRef = useRef(null);

  currentValueRef.current = numericValue;

  const stopHolding = () => {
    if (holdTimeoutRef.current !== null) {
      clearTimeout(holdTimeoutRef.current);
      holdTimeoutRef.current = null;
    }

    if (holdIntervalRef.current !== null) {
      clearInterval(holdIntervalRef.current);
      holdIntervalRef.current = null;
    }
  };

  useEffect(() => {
    return stopHolding;
  }, []);

  const changeValueBy = (difference) => {
    const nextValue = Math.max(
      0,
      currentValueRef.current + difference,
    );

    currentValueRef.current = nextValue;
    onChange(String(nextValue));

    if (difference < 0 && nextValue === 0) {
      stopHolding();
    }
  };

  const startHolding = (
    event,
    difference,
  ) => {
    event.preventDefault();

    stopHolding();
    changeValueBy(difference);

    try {
      event.currentTarget.setPointerCapture(
        event.pointerId,
      );
    } catch {
      // Capturarea pointerului nu este obligatorie.
    }

    holdTimeoutRef.current = setTimeout(() => {
      holdIntervalRef.current = setInterval(() => {
        changeValueBy(difference);
      }, 90);
    }, 400);
  };

  const handleKeyboardClick = (
    event,
    difference,
  ) => {
    /*
     * Click-urile produse de mouse/touch sunt deja
     * tratate prin onPointerDown. detail === 0 indica
     * de regula activarea prin tastatura.
     */
    if (event.detail === 0) {
      changeValueBy(difference);
    }
  };

  const handleInputChange = (event) => {
    const nextValue = event.target.value;

    if (nextValue === "" || /^\d+$/.test(nextValue)) {
      onChange(nextValue);
    }
  };

  return (
    <div className="filter-group compact-number-field">
      <label htmlFor={id}>{label}</label>

      <div className="number-stepper">
        <button
          type="button"
          className="number-stepper-button"
          onPointerDown={(event) =>
            startHolding(event, -1)
          }
          onPointerUp={stopHolding}
          onPointerCancel={stopHolding}
          onPointerLeave={stopHolding}
          onClick={(event) =>
            handleKeyboardClick(event, -1)
          }
          disabled={numericValue <= 0}
          aria-label={`Scade ${label}`}
          title="Apasa sau tine apasat"
        >
          −
        </button>

        <input
          id={id}
          type="number"
          min="0"
          step="1"
          inputMode="numeric"
          value={value}
          onChange={handleInputChange}
          onBlur={() => {
            if (value === "") {
              onChange("0");
            }
          }}
        />

        <button
          type="button"
          className="number-stepper-button"
          onPointerDown={(event) =>
            startHolding(event, 1)
          }
          onPointerUp={stopHolding}
          onPointerCancel={stopHolding}
          onPointerLeave={stopHolding}
          onClick={(event) =>
            handleKeyboardClick(event, 1)
          }
          aria-label={`Creste ${label}`}
          title="Apasa sau tine apasat"
        >
          +
        </button>
      </div>
    </div>
  );
};

function AdminPage() {
  const [errorMessage, setErrorMessage] = useState("");

  const [unavailableSupplies, setUnavailableSupplies] = useState([]);

  const [decisionSummary, setDecisionSummary] = useState(
    readStoredDecisionSummary,
  );

  const [decisionLoading, setDecisionLoading] = useState(false);

  const [decisionError, setDecisionError] = useState("");

  const [decisionUpdatedAt, setDecisionUpdatedAt] = useState(
    readStoredDecisionUpdatedAt,
  );

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
    sessionStorage.removeItem(DECISION_SUMMARY_STORAGE_KEY);
    sessionStorage.removeItem(DECISION_UPDATED_AT_STORAGE_KEY);
    globalThis.location.href = "/login";
  };

  const handleOpenStatistics = () => {
    globalThis.location.href = "/admin/statistics";
  };

  const handleOpenOrderHistory = () => {
    globalThis.location.href = "/admin/order-history";
  };

  const handleOpenStockConfiguration = () => {
    globalThis.location.href = "/admin/stock-configuration";
  };

  const handleOpenEmployees = () => {
    globalThis.location.href = "/admin/employees";
  };

  const loadAdminData = () => {
    setErrorMessage("");

    getUnavailableAuxiliarySupplies()
      .then((response) => {
        setUnavailableSupplies(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea articolelor lipsa:", error);

        setErrorMessage("Articolele de stoc lipsa nu au putut fi incarcate.");
      });
  };

  const loadLatestUnlabeledRecord = () => {
    setLabelLoading(true);
    setLabelError("");

    getLatestUnlabeledDecisionRecord()
      .then((response) => {
        const record = response.data;

        setLatestUnlabeledRecord(record);

        setObservedTrafficLevel(record.predictedTrafficLevel || "SCAZUT");

        setObservedDelayRisk(record.predictedDelayRisk || "SCAZUT");

        setActualWaiters(String(record.recommendedWaiters ?? 0));

        setActualKitchenStaff(String(record.recommendedKitchenStaff ?? 0));

        setActualBarStaff(String(record.recommendedBarStaff ?? 0));
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
        const updatedAt = new Date();

        setDecisionSummary(response.data);
        setDecisionUpdatedAt(updatedAt);

        sessionStorage.setItem(
          DECISION_SUMMARY_STORAGE_KEY,
          JSON.stringify(response.data),
        );

        sessionStorage.setItem(
          DECISION_UPDATED_AT_STORAGE_KEY,
          updatedAt.toISOString(),
        );

        loadLatestUnlabeledRecord();
      })
      .catch((error) => {
        console.error("Eroare la incarcarea predictiilor:", error);

        setDecisionError(
          "Predictiile nu au putut fi incarcate. " +
            "Verifica daca AI Service este pornit.",
        );
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

    if (
      actualWaiters === "" ||
      actualKitchenStaff === "" ||
      actualBarStaff === ""
    ) {
      setLabelError("Completeaza necesarul real observat pentru fiecare rol.");

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
      setLabelError("Necesarul observat trebuie sa fie zero sau mai mare.");

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
        setLabelMessage(
          `Inregistrarea #${latestUnlabeledRecord.id} ` +
            "a fost etichetata cu succes.",
        );

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
        setRetrainingMessage(
          response.data?.message || "Reantrenarea a fost finalizata.",
        );
      })
      .catch((error) => {
        const message =
          error.response?.data?.message ||
          "Modelele nu au putut fi reantrenate.";

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

  const staffingRows = decisionSummary
    ? staffingRoles.map((role) => {
        const deficit = decisionSummary[role.deficitField] ?? 0;

        return {
          ...role,
          active: decisionSummary[role.activeField] ?? 0,
          recommended: decisionSummary[role.recommendedField] ?? 0,
          deficit,
          status: getStaffingStatus(deficit),
        };
      })
    : [];

  const operationalRecommendations = decisionSummary
    ? buildOperationalRecommendations(decisionSummary)
    : [];

  const fallbackActive =
    decisionSummary?.trafficLevel === "NECUNOSCUT" ||
    decisionSummary?.delayRisk === "NECUNOSCUT";

  const hasPendingPreviousPrediction =
    !decisionSummary &&
    !decisionLoading &&
    latestUnlabeledRecord;

  return (
    <div className="admin-page">
      <header className="admin-header">
        <h1>Panou administrare</h1>

        <div className="admin-header-actions">
          <button
            type="button"
            className="admin-nav-button"
            onClick={handleOpenStatistics}
          >
            Statistici si rapoarte
          </button>

          <button
            type="button"
            className="admin-nav-button"
            onClick={handleOpenOrderHistory}
          >
            Istoric comenzi
          </button>

          <button
            type="button"
            className="admin-nav-button"
            onClick={handleOpenStockConfiguration}
          >
            Configurare stocuri
          </button>

          <button
            type="button"
            className="admin-nav-button"
            onClick={handleOpenEmployees}
          >
            Administrare angajati
          </button>

          <button
            type="button"
            className="logout-button"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <section className="admin-section decision-section">
        <div className="decision-section-header">
          <div>
            <h2>Sistem de decizie</h2>

            <p>
              Comparatie intre personalul aflat in tura si necesarul estimat de
              modelele AI.
            </p>
          </div>

          <button
            type="button"
            className="decision-refresh-button"
            onClick={loadDecisionSummary}
            disabled={decisionLoading}
          >
            {decisionLoading
              ? "Se actualizeaza..."
              : "Actualizeaza predictiile"}
          </button>
        </div>

        {decisionError && <p className="error-message">{decisionError}</p>}

        {fallbackActive && (
          <p className="decision-fallback-warning">
            AI Service nu a oferit o predictie valida. Personalul activ este
            afisat, dar deficitul nu poate fi evaluat momentan.
          </p>
        )}

        {decisionSummary && (
          <div className="decision-dashboard">
            <div className="decision-summary-grid">
              <article className="decision-summary-card">
                <span className="decision-card-label">
                  Nivel trafic estimat
                </span>

                <strong
                  className={`decision-level-badge ${getLevelClassName(
                    decisionSummary.trafficLevel,
                  )}`}
                >
                  {decisionSummary.trafficLevel}
                </strong>
              </article>

              <article className="decision-summary-card">
                <span className="decision-card-label">Risc de intarziere</span>

                <strong
                  className={`decision-level-badge ${getLevelClassName(
                    decisionSummary.delayRisk,
                  )}`}
                >
                  {decisionSummary.delayRisk}
                </strong>
              </article>

              <article className="decision-summary-card">
                <span className="decision-card-label">Deficit total</span>

                <strong className="decision-card-value">
                  {decisionSummary.totalStaffDeficit ?? 0}
                </strong>

                <small>angajati suplimentari necesari</small>
              </article>

              <article className="decision-summary-card">
                <span className="decision-card-label">Ultima actualizare</span>

                <strong className="decision-update-time">
                  {decisionUpdatedAt
                    ? decisionUpdatedAt.toLocaleTimeString()
                    : "Necunoscuta"}
                </strong>

                <small>
                  {decisionUpdatedAt
                    ? decisionUpdatedAt.toLocaleDateString()
                    : ""}
                </small>
              </article>
            </div>

            <div className="staffing-comparison">
              <div className="decision-subsection-heading">
                <div>
                  <h3>Personal activ vs recomandat</h3>

                  <p>
                    Personalul activ este calculat din turele pornite ale
                    angajatilor.
                  </p>
                </div>
              </div>

              <div className="staffing-table-wrapper">
                <table className="staffing-table">
                  <thead>
                    <tr>
                      <th>Rol</th>
                      <th>Activ</th>
                      <th>Recomandat</th>
                      <th>Situatie</th>
                    </tr>
                  </thead>

                  <tbody>
                    {staffingRows.map((row) => (
                      <tr key={row.key}>
                        <td>
                          <strong>{row.label}</strong>
                        </td>

                        <td>{row.active}</td>

                        <td>{row.recommended}</td>

                        <td>
                          <span
                            className={`staffing-status ${row.status.className}`}
                          >
                            {row.status.label}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="operational-recommendations">
              <div className="decision-subsection-heading">
                <div>
                  <h3>Recomandari operationale</h3>

                  <p>
                    Actiuni generate determinist pe baza predictiilor si a
                    deficitului calculat.
                  </p>
                </div>
              </div>

              <div className="recommendation-list">
                {operationalRecommendations.map((recommendation, index) => (
                  <div
                    key={`${recommendation.type}-${index}`}
                    className={`recommendation-item ${recommendation.type}`}
                  >
                    {recommendation.text}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {!decisionSummary && !decisionLoading && (
          <div
            className={`decision-empty-state ${
              hasPendingPreviousPrediction ? "has-pending-record" : ""
            }`}
          >
            {hasPendingPreviousPrediction ? (
              <>
                <strong>Exista o predictie anterioara neetichetata.</strong>

                <span>
                  Inregistrarea #{latestUnlabeledRecord.id} din {" "}
                  {latestUnlabeledRecord.createdAt
                    ? new Date(
                        latestUnlabeledRecord.createdAt,
                      ).toLocaleString()
                    : "data necunoscuta"} este afisata in sectiunea de mai
                  jos. Apasa „Actualizeaza predictiile” numai pentru o analiza
                  noua a situatiei curente.
                </span>
              </>
            ) : (
              <span>
                Apasa „Actualizeaza predictiile” pentru a analiza situatia
                operationala curenta.
              </span>
            )}
          </div>
        )}
      </section>

      <section className="admin-section">
        <h2>Rezultate reale pentru reantrenare</h2>

        <p>
          Completeaza rezultatele observate pentru ultima predictie. Pentru
          personal se introduce necesarul real constatat, nu simplul numar de
          angajati prezenti in tura.
        </p>

        <p className="decision-label-note">
          Personalul prezent este preluat automat din sistemul de ture. Valorile
          de mai jos reprezinta cati angajati ar fi fost necesari in realitate
          pentru functionarea corecta.
        </p>

        {labelLoading && <p>Se cauta ultima predictie neetichetata...</p>}

        {labelError && <p className="error-message">{labelError}</p>}

        {labelMessage && <p className="feedback-message">{labelMessage}</p>}

        {!labelLoading && !latestUnlabeledRecord && (
          <p>
            Nu exista momentan nicio predictie neetichetata. Apasa „Actualizeaza
            predictiile” pentru a crea una noua.
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
                    onChange={(event) =>
                      setObservedTrafficLevel(event.target.value)
                    }
                  >
                    <option value="SCAZUT">Scazut</option>

                    <option value="MEDIU">Mediu</option>

                    <option value="RIDICAT">Ridicat</option>
                  </select>
                </div>

                <div className="filter-group">
                  <label htmlFor="observed-delay">
                    Risc real de intarziere
                  </label>

                  <select
                    id="observed-delay"
                    value={observedDelayRisk}
                    onChange={(event) =>
                      setObservedDelayRisk(event.target.value)
                    }
                  >
                    <option value="SCAZUT">Scazut</option>

                    <option value="MEDIU">Mediu</option>

                    <option value="RIDICAT">Ridicat</option>
                  </select>
                </div>

                <NumericStepper
                  id="actual-waiters"
                  label="Ospatari necesari"
                  value={actualWaiters}
                  onChange={setActualWaiters}
                />

                <NumericStepper
                  id="actual-kitchen-staff"
                  label="Bucatarie necesara"
                  value={actualKitchenStaff}
                  onChange={setActualKitchenStaff}
                />

                <NumericStepper
                  id="actual-bar-staff"
                  label="Bar necesar"
                  value={actualBarStaff}
                  onChange={setActualBarStaff}
                />
              </div>

              <button type="submit" disabled={labelSaving}>
                {labelSaving
                  ? "Se salveaza..."
                  : "Salveaza etichetele observate"}
              </button>
            </form>
          </>
        )}

        <div className="retraining-area">
          <h3>Reantrenare modele</h3>

          <p>
            Reantrenarea este permisa dupa colectarea a cel putin 30 de
            inregistrari etichetate.
          </p>

          <button
            type="button"
            onClick={handleRetrainModels}
            disabled={retraining}
          >
            {retraining ? "Se reantreneaza..." : "Reantreneaza modelele"}
          </button>

          {retrainingMessage && (
            <p className="feedback-message">{retrainingMessage}</p>
          )}

          {retrainingError && (
            <p className="error-message">{retrainingError}</p>
          )}
        </div>
      </section>

      <section className="admin-section">
        <h2>Articole de stoc semnalate ca lipsa</h2>

        {unavailableSupplies.length === 0 ? (
          <p>Nu exista articole de stoc semnalate ca lipsa.</p>
        ) : (
          <div className="admin-grid">
            {unavailableSupplies.map((supply) => (
              <div key={supply.id} className="admin-card">
                <h3>{supply.name}</h3>

                <p>
                  Zona:{" "}
                  {stockTypeLabels[supply.stockType] ||
                    supply.stockType ||
                    "Necunoscuta"}
                </p>

                <p>
                  Categorie:{" "}
                  {categoryLabels[supply.category] || supply.category}
                </p>

                <p>
                  Cantitate: {supply.currentQuantity ?? 0}{" "}
                  {unitLabels[supply.baseUnit] || supply.baseUnit || ""}
                </p>

                <p>Status: lipsa in stoc</p>

                <p>
                  Semnalat la:{" "}
                  {supply.reportedAt
                    ? new Date(supply.reportedAt).toLocaleString()
                    : "necunoscut"}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default AdminPage;