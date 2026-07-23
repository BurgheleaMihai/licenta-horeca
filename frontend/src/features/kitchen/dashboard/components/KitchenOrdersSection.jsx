import { getKitchenItems } from "../utils/kitchenDashboardUtils";

function KitchenOrdersSection({ orders, onMarkItemsAsReady }) {
  return (
    <section className="kitchen-section">
      <h2>Preparate in lucru</h2>

      {orders.length === 0 ? (
        <p className="status-info">Nu exista preparate de gatit.</p>
      ) : (
        <div className="kitchen-grid">
          {orders.map((order, index) => {
            const kitchenItems = getKitchenItems(order);

            return (
              <div key={order.id} className="kitchen-card">
                <h3>Comanda {index + 1}</h3>

                <p>Status comanda: {order.status}</p>

                <div className="order-items-list">
                  <strong>Preparate:</strong>

                  {kitchenItems.map((item) => (
                    <p key={item.id}>
                      {item.quantity} x {item.product?.name} - {item.status}
                    </p>
                  ))}
                </div>

                <button
                  type="button"
                  className="kitchen-button"
                  onClick={() => onMarkItemsAsReady(order)}
                >
                  Marcheaza preparatele ca gata
                </button>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default KitchenOrdersSection;
