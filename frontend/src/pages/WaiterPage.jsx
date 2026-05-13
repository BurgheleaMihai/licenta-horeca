import { useEffect, useState } from "react";
import { getActiveOrders, updateOrderStatus } from "../api/orderApi";
import { getAllTables } from "../api/tableApi";
import {
  getActiveTableSessions,
  createTableSessionForTable
} from "../api/tableSessionApi";

function WaiterPage() {
  const [tables, setTables] = useState([]);
  const [orders, setOrders] = useState([]);
  const [activeSessions, setActiveSessions] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadTables();
    loadOrders();
    loadActiveSessions();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  const loadTables = () => {
    getAllTables()
      .then((response) => {
        setTables(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea meselor:", error);
        setErrorMessage("Mesele nu au putut fi incarcate.");
      });
  };

  const loadOrders = () => {
    getActiveOrders()
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor:", error);
        setErrorMessage("Comenzile nu au putut fi incarcate.");
      });
  };

  const loadActiveSessions = () => {
    getActiveTableSessions()
      .then((response) => {
        setActiveSessions(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea sesiunilor active:", error);
        setErrorMessage("Sesiunile active nu au putut fi incarcate.");
      });
  };

  const getActiveSessionForTable = (tableId) => {
    return activeSessions.find(
      (session) => session.restaurantTable?.id === tableId
    );
  };

  const handleCreateOrderClick = (table) => {
    const activeSession = getActiveSessionForTable(table.id);

    if (activeSession) {
      alert(`Se poate crea comanda pentru Masa ${table.tableNumber}.`);
      return;
    }

    const confirmCreateSession = globalThis.confirm(
      `Masa ${table.tableNumber} nu are sesiune activă. Dorești să creezi o sesiune nouă pentru această masă?`
    );

    if (!confirmCreateSession) {
      return;
    }

    createTableSessionForTable(table.id)
      .then(() => {
        loadActiveSessions();
        alert(`Sesiunea pentru Masa ${table.tableNumber} a fost creată.`);
      })
      .catch((error) => {
        console.error("Eroare la crearea sesiunii:", error);
        setErrorMessage("Sesiunea pentru masă nu a putut fi creată.");
      });
  };

  const markOrderAsServed = (orderId) => {
    updateOrderStatus(orderId, "SERVITA")
      .then(() => {
        loadOrders();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea statusului:", error);
        setErrorMessage("Statusul comenzii nu a putut fi actualizat.");
      });
  };

  const sendOrderToPreparation = (orderId) => {
    updateOrderStatus(orderId, "IN_PREPARARE")
      .then(() => {
        loadOrders();
      })
      .catch((error) => {
        console.error("Eroare la trimiterea comenzii la preparare:", error);
        setErrorMessage("Comanda nu a putut fi trimisa la preparare.");
      });
  };

  const getFoodItems = (order) => {
    return (
      order.items?.filter(
        (item) => item.product?.category?.name !== "Bauturi"
      ) || []
    );
  };

  const getDrinkItems = (order) => {
    return (
      order.items?.filter(
        (item) => item.product?.category?.name === "Bauturi"
      ) || []
    );
  };

  return (
    <div className="waiter-page">
      <header className="waiter-header">
        <h1>Panou ospatar</h1>

        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <section className="waiter-section">
        <h2>Mese restaurant</h2>

        <div className="waiter-grid">
          {tables.map((table) => {
            const activeSession = getActiveSessionForTable(table.id);

            return (
              <div key={table.id} className="waiter-card">
                <h3>Masa {table.tableNumber}</h3>

                <p>Capacitate: {table.capacity} persoane</p>

                <p>
                  Stare masă:{" "}
                  <strong>
                    {activeSession ? "Sesiune activă" : "Fără sesiune activă"}
                  </strong>
                </p>

                <button
                  className="waiter-button"
                  onClick={() => handleCreateOrderClick(table)}
                >
                  Creeaza comanda
                </button>
              </div>
            );
          })}
        </div>
      </section>

      <section className="waiter-section">
        <h2>Comenzi active</h2>

        <div className="waiter-grid">
          {orders.map((order) => (
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

                {getFoodItems(order).length > 0 ? (
                  getFoodItems(order).map((item) => (
                    <p key={item.id}>
                      {item.quantity} x {item.product?.name} - {item.status} -{" "}
                      {Number(item.subtotal).toFixed(2)} lei
                    </p>
                  ))
                ) : (
                  <p>Nu exista preparate in comanda.</p>
                )}

                <strong className="order-items-subtitle">Bauturi:</strong>

                {getDrinkItems(order).length > 0 ? (
                  getDrinkItems(order).map((item) => (
                    <p key={item.id}>
                      {item.quantity} x {item.product?.name} - {item.status} -{" "}
                      {Number(item.subtotal).toFixed(2)} lei
                    </p>
                  ))
                ) : (
                  <p>Nu exista bauturi in comanda.</p>
                )}
              </div>

              {order.status === "NOUA" && (
                <button
                  className="waiter-button"
                  onClick={() => sendOrderToPreparation(order.id)}
                >
                  Trimite la preparare
                </button>
              )}

              {order.status === "IN_PREPARARE" && (
                <p className="status-info">Comanda este in preparare.</p>
              )}

              {order.status === "GATA" && (
                <button
                  className="waiter-button"
                  onClick={() => markOrderAsServed(order.id)}
                >
                  Marcheaza ca servita
                </button>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

export default WaiterPage;