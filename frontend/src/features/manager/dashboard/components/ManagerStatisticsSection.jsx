/*
 * Afiseaza filtrarea si indicatorii
 * pentru perioada selectata.
 */
function ManagerStatisticsSection({
  selectedDate,
  startTime,
  endTime,
  appliedFilters,
  statisticsLoading,
  statistics,
  onSelectedDateChange,
  onStartTimeChange,
  onEndTimeChange,
  onSubmit,
  onReset,
}) {
  return (
    <section className="manager-section">
      <h2>Indicatori pe perioada selectata</h2>

      <form className="manager-statistics-filter" onSubmit={onSubmit}>
        <div className="filter-group">
          <label htmlFor="statistics-date">Data</label>

          <input
            id="statistics-date"
            type="date"
            value={selectedDate}
            onChange={onSelectedDateChange}
          />
        </div>

        <div className="filter-group">
          <label htmlFor="statistics-start-time">Ora de inceput</label>

          <input
            id="statistics-start-time"
            type="time"
            value={startTime}
            onChange={onStartTimeChange}
          />
        </div>

        <div className="filter-group">
          <label htmlFor="statistics-end-time">Ora de sfarsit</label>

          <input
            id="statistics-end-time"
            type="time"
            value={endTime}
            onChange={onEndTimeChange}
          />
        </div>

        <div className="manager-filter-actions">
          <button type="submit" disabled={statisticsLoading}>
            {statisticsLoading ? "Se incarca..." : "Aplica filtrul"}
          </button>

          <button
            type="button"
            className="secondary-button"
            onClick={onReset}
            disabled={statisticsLoading}
          >
            Astazi
          </button>
        </div>
      </form>

      <p className="manager-filter-summary">
        Data: <strong>{appliedFilters.date}</strong>, interval:{" "}
        <strong>
          {appliedFilters.startTime} - {appliedFilters.endTime}
        </strong>
      </p>

      <p className="manager-filter-note">
        Comenzile active reprezinta situatia curenta. Comenzile servite,
        vanzarile si ratingul sunt calculate pentru intervalul selectat.
      </p>

      <div className="manager-grid">
        <div className="manager-card">
          <h3>Comenzi active acum</h3>

          <p>{statistics.activeOrders}</p>
        </div>

        <div className="manager-card">
          <h3>Comenzi servite</h3>

          <p>{statistics.servedOrders}</p>
        </div>

        <div className="manager-card">
          <h3>Vanzari</h3>

          <p>{Number(statistics.sales ?? 0).toFixed(2)} lei</p>
        </div>

        <div className="manager-card">
          <h3>Rating mediu</h3>

          <p>{Number(statistics.averageRating ?? 0).toFixed(2)} / 5</p>
        </div>
      </div>
    </section>
  );
}

export default ManagerStatisticsSection;
