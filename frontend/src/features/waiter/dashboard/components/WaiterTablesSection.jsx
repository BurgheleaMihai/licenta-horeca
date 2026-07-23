import { findActiveSessionForTable } from "../utils/waiterDashboardUtils";

function WaiterTablesSection({
  tables,
  activeSessions,
  onOpenSession,
  onCreateOrder,
  onCloseSession,
}) {
  return (
    <section className="waiter-section">
      <h2>Mese restaurant</h2>

      <div className="waiter-grid">
        {tables.map((table) => {
          const activeSession = findActiveSessionForTable(
            activeSessions,
            table.id,
          );

          return (
            <div key={table.id} className="waiter-card">
              <h3>Masa {table.tableNumber}</h3>

              <p>Capacitate: {table.capacity} persoane</p>

              <p>
                Stare masa:{" "}
                <strong>{activeSession ? "Deschisa" : "Inchisa"}</strong>
              </p>

              {!activeSession && (
                <button
                  type="button"
                  className="waiter-button"
                  onClick={() => onOpenSession(table)}
                >
                  Deschide masa
                </button>
              )}

              {activeSession && (
                <>
                  <button
                    type="button"
                    className="waiter-button"
                    onClick={() => onCreateOrder(table)}
                  >
                    Creeaza comanda
                  </button>

                  <button
                    type="button"
                    className="waiter-button"
                    onClick={() => onCloseSession(table, activeSession)}
                  >
                    Inchide masa / Platita
                  </button>
                </>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

export default WaiterTablesSection;
