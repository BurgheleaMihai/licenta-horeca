/*
 * Afiseaza primele zece produse vandute
 * in perioada selectata.
 */
function TopSellingProductsSection({ loading, productSalesList }) {
  return (
    <section className="admin-statistics-section">
      <h2>Top produse vandute</h2>

      {loading ? (
        <p>Se incarca produsele...</p>
      ) : productSalesList.length === 0 ? (
        <p>Nu exista produse vandute in perioada selectata.</p>
      ) : (
        <div className="admin-statistics-grid">
          {productSalesList.slice(0, 10).map((product, index) => (
            <div key={product.name} className="admin-statistics-card">
              <h3>
                #{index + 1} {product.name}
              </h3>

              <p>Cantitate vanduta: {product.quantity}</p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default TopSellingProductsSection;
