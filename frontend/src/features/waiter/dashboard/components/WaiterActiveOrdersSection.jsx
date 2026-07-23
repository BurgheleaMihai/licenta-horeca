import { ORDER_STATUS } from "../constants/waiterDashboardConstants";
import { getDrinkItems, getFoodItems } from "../utils/waiterDashboardUtils";

function WaiterActiveOrdersSection({
  orders,
  onSendToPreparation,
  onMarkAsServed,
}) {
  return (
    <section className="waiter-section">
      <h2>Comenzi active</h2>

      {orders.length === 0 ? (
        <p>Nu exista comenzi active.</p>
      ) : (
        <div className="waiter-grid">
          {orders.map((order) => {
            const foodItems = getFoodItems(order);

            const drinkItems = getDrinkItems(order);

            return (
              <div key={order.id} className="waiter-card">
                <h3>Comanda #{order.id}</h3>

                <p>
                  Masa:{" "}
                  {order.tableSession?.restaurantTable?.tableNumber ||
                    "necunoscuta"}
                </p>

                <p>Status comanda: {order.status}</p>

                <p>Total: {Number(order.totalPrice).toFixed(2)} lei</p>

                <div className="order-items-list">
                  <strong>Preparate:</strong>

                  {foodItems.length > 0 ? (
                    foodItems.map((item) => (
                      <p key={item.id}>
                        {item.quantity} x {item.product?.name}
                        {" - "}
                        {item.status}
                        {" - "}
                        {Number(item.subtotal).toFixed(2)} lei
                      </p>
                    ))
                  ) : (
                    <p>Nu exista preparate in comanda.</p>
                  )}

                  <strong className="order-items-subtitle">Bauturi:</strong>

                  {drinkItems.length > 0 ? (
                    drinkItems.map((item) => (
                      <p key={item.id}>
                        {item.quantity} x {item.product?.name}
                        {" - "}
                        {item.status}
                        {" - "}
                        {Number(item.subtotal).toFixed(2)} lei
                      </p>
                    ))
                  ) : (
                    <p>Nu exista bauturi in comanda.</p>
                  )}
                </div>

                {order.status === ORDER_STATUS.NEW && (
                  <button
                    type="button"
                    className="waiter-button"
                    onClick={() => onSendToPreparation(order.id)}
                  >
                    Trimite la preparare
                  </button>
                )}

                {order.status === ORDER_STATUS.IN_PREPARATION && (
                  <p className="status-info">Comanda este in preparare.</p>
                )}

                {order.status === ORDER_STATUS.READY && (
                  <button
                    type="button"
                    className="waiter-button"
                    onClick={() => onMarkAsServed(order.id)}
                  >
                    Marcheaza ca servita
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default WaiterActiveOrdersSection;
