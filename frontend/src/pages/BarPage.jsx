import { useEffect, useState } from "react";
import { getBarOrders, updateOrderItemStatus } from "../api/orderApi";

function BarPage() {
  const [orders, setOrders] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadBarOrders();
  }, []);

  const handleLogout = () => {
      localStorage.removeItem("user");
      window.location.href = "/login";
  };

  const loadBarOrders = () => {
    getBarOrders()
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor pentru bar:", error);
        setErrorMessage("Comenzile pentru bar nu au putut fi incarcate.");
      });
  };

  const getBarItems = (order) => {
    return order.items?.filter(
      (item) =>
        item.product?.category?.name === "Bauturi" &&
        item.status !== "GATA"
    ) || [];
  };

  const visibleOrders = orders.filter((order) => getBarItems(order).length > 0);

  const markBarItemsAsReady = (order) => {
    const barItems = getBarItems(order);

    const updateRequests = barItems.map((item) =>
      updateOrderItemStatus(item.id, "GATA")
    );

    Promise.all(updateRequests)
      .then(() => {
        loadBarOrders();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea bauturilor:", error);
        setErrorMessage("Bauturile nu au putut fi marcate ca gata.");
      });
  };

  return (
    <div className="bar-page">
      <header className="bar-header">
        <h1>Panou bar</h1>
        <p>
          Aceasta pagina este folosita pentru vizualizarea bauturilor care trebuie pregatite.
        </p>

        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {errorMessage && (
        <p className="error-message">{errorMessage}</p>
      )}

      <section className="bar-section">
        <h2>Bauturi de pregatit</h2>

        {visibleOrders.length === 0 ? (
          <p className="status-info">Nu exista bauturi de pregatit.</p>
        ) : (
          <div className="bar-grid">
            {visibleOrders.map((order, index) => (
              <div key={order.id} className="bar-card">
                <h3>Comanda {index + 1}</h3>

                <p>Status comanda: {order.status}</p>

                <div className="order-items-list">
                  <strong>Bauturi:</strong>

                  {getBarItems(order).map((item) => (
                    <p key={item.id}>
                      {item.quantity} x {item.product?.name} - {item.status}
                    </p>
                  ))}
                </div>

                <button
                  className="bar-button"
                  onClick={() => markBarItemsAsReady(order)}
                >
                  Marcheaza bauturile ca gata
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default BarPage;