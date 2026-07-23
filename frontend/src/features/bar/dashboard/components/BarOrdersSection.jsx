import { getBarItems } from "../utils/barDashboardUtils";

function BarOrdersSection({ orders, onMarkItemsAsReady }) {
  return (
    <section className="bar-section">
      <h2>Bauturi de pregatit</h2>

      {orders.length === 0 ? (
        <p className="status-info">Nu exista bauturi de pregatit.</p>
      ) : (
        <div className="bar-grid">
          {orders.map((order, index) => {
            const barItems = getBarItems(order);

            return (
              <div key={order.id} className="bar-card">
                <h3>Comanda {index + 1}</h3>

                <p>Status comanda: {order.status}</p>

                <div className="order-items-list">
                  <strong>Bauturi:</strong>

                  {barItems.map((item) => (
                    <p key={item.id}>
                      {item.quantity} x {item.product?.name} - {item.status}
                    </p>
                  ))}
                </div>

                <button
                  type="button"
                  className="bar-button"
                  onClick={() => onMarkItemsAsReady(order)}
                >
                  Marcheaza bauturile ca gata
                </button>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default BarOrdersSection;
