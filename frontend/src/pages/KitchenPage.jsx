import { useEffect, useState } from "react";
import { getKitchenOrders, updateOrderItemStatus } from "../api/orderApi";

function KitchenPage() {
  const [orders, setOrders] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadKitchenOrders();
  }, []);

  const handleLogout = () => {
      localStorage.removeItem("user");
      window.location.href = "/login";
  };

  const loadKitchenOrders = () => {
    getKitchenOrders()
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor pentru bucatarie:", error);
        setErrorMessage("Comenzile pentru bucatarie nu au putut fi incarcate.");
      });
  };

  const getKitchenItems = (order) => {
    return order.items?.filter(
      (item) =>
        item.product?.category?.name !== "Bauturi" &&
        item.status !== "GATA"
    ) || [];
  };

  const visibleOrders = orders.filter((order) => getKitchenItems(order).length > 0);

  const markKitchenItemsAsReady = (order) => {
    const kitchenItems = getKitchenItems(order);

    const updateRequests = kitchenItems.map((item) =>
      updateOrderItemStatus(item.id, "GATA")
    );

    Promise.all(updateRequests)
      .then(() => {
        loadKitchenOrders();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea produselor de bucatarie:", error);
        setErrorMessage("Produsele de bucatarie nu au putut fi marcate ca gata.");
      });
  };

  return (
    <div className="kitchen-page">
      <header className="kitchen-header">
        <h1>Panou bucatarie</h1>
        <p>
          Aceasta pagina este folosita pentru vizualizarea preparatelor aflate in lucru.
        </p>

        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {errorMessage && (
        <p className="error-message">{errorMessage}</p>
      )}

      <section className="kitchen-section">
        <h2>Preparate in lucru</h2>

        {visibleOrders.length === 0 ? (
          <p className="status-info">Nu exista preparate de gatit.</p>
        ) : (
          <div className="kitchen-grid">
            {visibleOrders.map((order, index) => (
              <div key={order.id} className="kitchen-card">
                <h3>Comanda {index + 1}</h3>

                <p>Status comanda: {order.status}</p>

                <div className="order-items-list">
                  <strong>Preparate:</strong>

                  {getKitchenItems(order).map((item) => (
                    <p key={item.id}>
                      {item.quantity} x {item.product?.name} - {item.status}
                    </p>
                  ))}
                </div>

                <button
                  className="kitchen-button"
                  onClick={() => markKitchenItemsAsReady(order)}
                >
                  Marcheaza preparatele ca gata
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default KitchenPage;