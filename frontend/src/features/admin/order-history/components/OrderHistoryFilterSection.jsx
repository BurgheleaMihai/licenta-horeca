/*
 * Formularul folosit pentru filtrarea
 * istoricului comenzilor.
 */
function OrderHistoryFilterSection({
  filters,
  tableNumbers,
  loading,
  onFilterChange,
  onSubmit,
  onShowRecentOrders,
  onReset,
  onReload,
}) {
  return (
    <section className="admin-order-history-section">
      <h2>Cauta in istoric</h2>

      <form className="admin-order-history-filter" onSubmit={onSubmit}>
        <div className="admin-order-history-fields">
          <div className="filter-group">
            <label htmlFor="history-start-date">Data de inceput</label>

            <input
              id="history-start-date"
              type="date"
              value={filters.startDate}
              onChange={(event) =>
                onFilterChange("startDate", event.target.value)
              }
            />
          </div>

          <div className="filter-group">
            <label htmlFor="history-end-date">Data de sfarsit</label>

            <input
              id="history-end-date"
              type="date"
              value={filters.endDate}
              onChange={(event) =>
                onFilterChange("endDate", event.target.value)
              }
            />
          </div>

          <div className="filter-group">
            <label htmlFor="history-start-time">Ora de inceput</label>

            <input
              id="history-start-time"
              type="time"
              value={filters.startTime}
              onChange={(event) =>
                onFilterChange("startTime", event.target.value)
              }
            />
          </div>

          <div className="filter-group">
            <label htmlFor="history-end-time">Ora de sfarsit</label>

            <input
              id="history-end-time"
              type="time"
              value={filters.endTime}
              onChange={(event) =>
                onFilterChange("endTime", event.target.value)
              }
            />
          </div>

          <div className="filter-group">
            <label htmlFor="history-status">Status</label>

            <select
              id="history-status"
              value={filters.status}
              onChange={(event) => onFilterChange("status", event.target.value)}
            >
              <option value="ALL_CLOSED">Servite si anulate</option>

              <option value="SERVITA">Servita</option>

              <option value="ANULATA">Anulata</option>
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="history-table">Masa</label>

            <select
              id="history-table"
              value={filters.tableNumber}
              onChange={(event) =>
                onFilterChange("tableNumber", event.target.value)
              }
            >
              <option value="ALL">Toate mesele</option>

              {tableNumbers.map((tableNumber) => (
                <option key={tableNumber} value={String(tableNumber)}>
                  Masa {tableNumber}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="history-min-value">Valoare minima</label>

            <input
              id="history-min-value"
              type="number"
              min="0"
              step="0.01"
              value={filters.minValue}
              placeholder="0 lei"
              onChange={(event) =>
                onFilterChange("minValue", event.target.value)
              }
            />
          </div>

          <div className="filter-group">
            <label htmlFor="history-max-value">Valoare maxima</label>

            <input
              id="history-max-value"
              type="number"
              min="0"
              step="0.01"
              value={filters.maxValue}
              placeholder="Fara limita"
              onChange={(event) =>
                onFilterChange("maxValue", event.target.value)
              }
            />
          </div>
        </div>

        <div className="admin-order-history-actions">
          <button type="submit" className="history-primary-button">
            Aplica filtrele
          </button>

          <button
            type="button"
            className="history-primary-button"
            onClick={onShowRecentOrders}
          >
            Ultimele 5 comenzi
          </button>

          <button
            type="button"
            className="history-secondary-button"
            onClick={onReset}
          >
            Reseteaza
          </button>

          <button
            type="button"
            className="history-secondary-button"
            onClick={onReload}
            disabled={loading}
          >
            {loading ? "Se incarca..." : "Reincarca datele"}
          </button>
        </div>
      </form>
    </section>
  );
}

export default OrderHistoryFilterSection;
