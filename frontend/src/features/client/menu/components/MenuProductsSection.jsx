function MenuProductsSection({ products }) {
  return (
    <>
      <p className="results-info">Produse afisate: {products.length}</p>

      <section className="product-grid">
        {products.map((product) => (
          <div
            key={product.id}
            className={`product-card ${product.available ? "" : "unavailable"}`}
          >
            <div className="product-card-header">
              <span className="product-category">
                {product.category?.name || "Fara categorie"}
              </span>

              <span
                className={product.available ? "available" : "not-available"}
              >
                {product.available ? "Disponibil" : "Indisponibil"}
              </span>
            </div>

            <h2>{product.name}</h2>

            <p>{product.description}</p>

            <div className="product-card-footer">
              <strong>{Number(product.price).toFixed(2)} lei</strong>
            </div>
          </div>
        ))}
      </section>
    </>
  );
}

export default MenuProductsSection;
