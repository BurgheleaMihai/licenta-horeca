import { getLevelClassName } from "../utils/decisionUtils";

/*
 * Afiseaza rezultatele sistemului de decizie.
 *
 * Componenta nu incarca date si nu modifica
 * direct state-ul paginii.
 *
 * Toate informatiile sunt primite prin props
 * de la AdminPage.
 */
function DecisionDashboardSection({
  decisionSummary,
  decisionUpdatedAt,
  decisionLoading,
  decisionError,
  fallbackActive,
  staffingRows,
  operationalRecommendations,
  hasPendingPreviousPrediction,
  latestUnlabeledRecord,
  onRefresh,
}) {
  return (
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
          onClick={onRefresh}
          disabled={decisionLoading}
        >
          {decisionLoading ? "Se actualizeaza..." : "Actualizeaza predictiile"}
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
              <span className="decision-card-label">Nivel trafic estimat</span>

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
                Inregistrarea #{latestUnlabeledRecord.id} din{" "}
                {latestUnlabeledRecord.createdAt
                  ? new Date(latestUnlabeledRecord.createdAt).toLocaleString()
                  : "data necunoscuta"}{" "}
                este afisata in sectiunea de mai jos. Apasa „Actualizeaza
                predictiile” numai pentru o analiza noua a situatiei curente.
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
  );
}

export default DecisionDashboardSection;
