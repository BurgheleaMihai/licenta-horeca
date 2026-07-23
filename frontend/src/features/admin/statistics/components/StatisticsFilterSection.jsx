import { getPeriodLabel } from "../utils/statisticsUtils";

/*
 * Formularul de filtrare a statisticilor
 * si rezumatul filtrelor aplicate.
 */
function StatisticsFilterSection({
  period,
  customStartDate,
  customEndDate,
  startTime,
  endTime,
  appliedFilters,
  onPeriodChange,
  onCustomStartDateChange,
  onCustomEndDateChange,
  onStartTimeChange,
  onEndTimeChange,
  onSubmit,
  onReset,
}) {
  return (
    <section className="admin-statistics-section">
      <h2>Filtre statistice</h2>

      <form className="admin-statistics-filter" onSubmit={onSubmit}>
        <div className="filter-group">
          <label htmlFor="statistics-period">Perioada</label>

          <select
            id="statistics-period"
            value={period}
            onChange={onPeriodChange}
          >
            <option value="TODAY">Astazi</option>

            <option value="LAST_7_DAYS">Ultimele 7 zile</option>

            <option value="CURRENT_MONTH">Luna curenta</option>

            <option value="CUSTOM">Interval personalizat</option>
          </select>
        </div>

        {period === "CUSTOM" && (
          <>
            <div className="filter-group">
              <label htmlFor="custom-start-date">Data de inceput</label>

              <input
                id="custom-start-date"
                type="date"
                value={customStartDate}
                onChange={onCustomStartDateChange}
              />
            </div>

            <div className="filter-group">
              <label htmlFor="custom-end-date">Data de sfarsit</label>

              <input
                id="custom-end-date"
                type="date"
                value={customEndDate}
                onChange={onCustomEndDateChange}
              />
            </div>
          </>
        )}

        <div className="filter-group">
          <label htmlFor="admin-start-time">Ora de inceput</label>

          <input
            id="admin-start-time"
            type="time"
            value={startTime}
            onChange={onStartTimeChange}
          />
        </div>

        <div className="filter-group">
          <label htmlFor="admin-end-time">Ora de sfarsit</label>

          <input
            id="admin-end-time"
            type="time"
            value={endTime}
            onChange={onEndTimeChange}
          />
        </div>

        <div className="admin-statistics-actions">
          <button type="submit">Aplica filtrul</button>

          <button type="button" className="secondary-button" onClick={onReset}>
            Reseteaza
          </button>
        </div>
      </form>

      <p className="admin-statistics-summary">
        Perioada: <strong>{getPeriodLabel(appliedFilters)}</strong>, interval
        orar:{" "}
        <strong>
          {appliedFilters.startTime} - {appliedFilters.endTime}
        </strong>
      </p>
    </section>
  );
}

export default StatisticsFilterSection;
