import NumericStepper from "./NumericStepper";

/*
 * Afiseaza formularul pentru salvarea rezultatelor
 * reale si zona pentru reantrenarea modelelor AI.
 *
 * State-urile si apelurile API raman gestionate
 * de AdminPage.
 */
function DecisionTrainingSection({
  labelLoading,
  labelError,
  labelMessage,
  latestUnlabeledRecord,
  observedTrafficLevel,
  setObservedTrafficLevel,
  observedDelayRisk,
  setObservedDelayRisk,
  actualWaiters,
  setActualWaiters,
  actualKitchenStaff,
  setActualKitchenStaff,
  actualBarStaff,
  setActualBarStaff,
  labelSaving,
  onLabelSubmit,
  retraining,
  retrainingMessage,
  retrainingError,
  onRetrainModels,
}) {
  return (
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

          <form className="decision-label-form" onSubmit={onLabelSubmit}>
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
              {labelSaving ? "Se salveaza..." : "Salveaza etichetele observate"}
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

        <button type="button" onClick={onRetrainModels} disabled={retraining}>
          {retraining ? "Se reantreneaza..." : "Reantreneaza modelele"}
        </button>

        {retrainingMessage && (
          <p className="feedback-message">{retrainingMessage}</p>
        )}

        {retrainingError && <p className="error-message">{retrainingError}</p>}
      </div>
    </section>
  );
}

export default DecisionTrainingSection;
