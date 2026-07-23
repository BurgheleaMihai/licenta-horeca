/*
 * Afiseaza principalii indicatori calculati
 * pentru perioada selectata.
 */
function StatisticsIndicatorsSection({ loading, statistics }) {
  return (
    <section className="admin-statistics-section">
      <h2>Indicatori principali</h2>

      {loading ? (
        <p>Se incarca statisticile...</p>
      ) : (
        <div className="admin-statistics-grid">
          <div className="admin-statistics-card">
            <h3>Total comenzi</h3>

            <p>{statistics.totalOrders}</p>
          </div>

          <div className="admin-statistics-card">
            <h3>Comenzi servite</h3>

            <p>{statistics.servedOrders}</p>
          </div>

          <div className="admin-statistics-card">
            <h3>Comenzi anulate</h3>

            <p>{statistics.cancelledOrders}</p>
          </div>

          <div className="admin-statistics-card">
            <h3>Venituri</h3>

            <p>{statistics.sales.toFixed(2)} lei</p>
          </div>

          <div className="admin-statistics-card">
            <h3>Valoare medie pe comanda</h3>

            <p>{statistics.averageOrderValue.toFixed(2)} lei</p>
          </div>

          <div className="admin-statistics-card">
            <h3>Produse vandute</h3>

            <p>{statistics.totalProductsSold}</p>
          </div>

          <div className="admin-statistics-card">
            <h3>Rating mediu</h3>

            <p>{statistics.averageRating.toFixed(2)} / 5</p>
          </div>
        </div>
      )}
    </section>
  );
}

export default StatisticsIndicatorsSection;
