/*
 * Afiseaza produsele cu cele mai multe
 * vanzari pentru sprijinirea aprovizionarii.
 */
function ManagerStockRecommendationsSection({ productSalesList }) {
  return (
    <section className="manager-section">
      <h2>Recomandari pentru stocul de produse</h2>

      {productSalesList.length === 0 ? (
        <p>Nu exista suficiente date pentru recomandari.</p>
      ) : (
        <div className="manager-grid">
          {productSalesList.slice(0, 3).map((product) => (
            <div key={product.name} className="manager-card">
              <h3>{product.name}</h3>

              <p>
                Produsul a fost vandut de <strong>{product.quantity}</strong>{" "}
                ori.
              </p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default ManagerStockRecommendationsSection;
