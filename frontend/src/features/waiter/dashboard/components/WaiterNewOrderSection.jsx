function WaiterNewOrderSection({
  selectedTable,
  products,
  productQuantities,
  savingOrder,
  onQuantityChange,
  onSubmitOrder,
  onCancelOrder,
}) {
  return (
    <section className="waiter-section">
      <h2>Comanda noua pentru Masa {selectedTable.tableNumber}</h2>

      <div className="waiter-grid">
        {products.map((product) => (
          <div key={product.id} className="waiter-card">
            <h3>{product.name}</h3>

            <p>
              Categorie: {product.category?.name || "Categorie necunoscuta"}
            </p>

            <p>Pret: {Number(product.price).toFixed(2)} lei</p>

            <label htmlFor={`quantity-${product.id}`}>Cantitate:</label>

            <input
              id={`quantity-${product.id}`}
              type="number"
              min="0"
              value={productQuantities[product.id] || 0}
              onChange={(event) =>
                onQuantityChange(product.id, event.target.value)
              }
            />
          </div>
        ))}
      </div>

      <button
        type="button"
        className="waiter-button"
        onClick={onSubmitOrder}
        disabled={savingOrder}
      >
        {savingOrder ? "Se salveaza..." : "Salveaza comanda"}
      </button>

      <button
        type="button"
        className="waiter-button"
        onClick={onCancelOrder}
        disabled={savingOrder}
      >
        Anuleaza
      </button>
    </section>
  );
}

export default WaiterNewOrderSection;
