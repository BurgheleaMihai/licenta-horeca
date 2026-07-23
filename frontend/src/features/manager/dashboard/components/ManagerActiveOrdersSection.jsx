/*
 * Afiseaza comenzile care sunt inca active.
 */
function ManagerActiveOrdersSection({ activeOrders }) {
  return (
    <section className="manager-section">
      <h2>Comenzi active</h2>

      {activeOrders.length === 0 ? (
        <p>Nu exista comenzi active.</p>
      ) : (
        <div className="manager-grid">
          {activeOrders.map((order) => (
            <div key={order.id} className="manager-card">
              <h3>Comanda #{order.id}</h3>

              <p>Status: {order.status}</p>

              <p>Total: {Number(order.totalPrice).toFixed(2)} lei</p>

              <p>
                Masa:{" "}
                {order.tableSession?.restaurantTable?.tableNumber ||
                  "necunoscuta"}
              </p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default ManagerActiveOrdersSection;
