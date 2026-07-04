import { useEffect, useState } from "react";
import {
createOrder,
getActiveOrders,
updateOrderStatus,
} from "../api/orderApi";
import { getAvailableProducts } from "../api/productApi";
import { getAllTables } from "../api/tableApi";
import {
closeTableSession,
createTableSessionForTable,
getActiveTableSessions,
} from "../api/tableSessionApi";

function WaiterPage() {
const [tables, setTables] = useState([]);
const [orders, setOrders] = useState([]);
const [activeSessions, setActiveSessions] = useState([]);
const [products, setProducts] = useState([]);

const [selectedTable, setSelectedTable] = useState(null);
const [selectedSession, setSelectedSession] = useState(null);
const [productQuantities, setProductQuantities] = useState({});

const [errorMessage, setErrorMessage] = useState("");
const [orderMessage, setOrderMessage] = useState("");
const [savingOrder, setSavingOrder] = useState(false);

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

const loadProducts = () => {
getAvailableProducts()
.then((response) => {
setProducts(response.data);
})
.catch((error) => {
console.error("Eroare la incarcarea produselor:", error);
setErrorMessage("Produsele nu au putut fi incarcate.");
});
};

useEffect(() => {
loadTables();
loadOrders();
loadActiveSessions();
loadProducts();
}, []);

const handleLogout = () => {
localStorage.removeItem("user");
globalThis.location.href = "/login";
};

const getActiveSessionForTable = (tableId) => {
return activeSessions.find(
(session) => session.restaurantTable?.id === tableId
);
};

const openOrderForm = (table, session) => {
setSelectedTable(table);
setSelectedSession(session);
setProductQuantities({});
setOrderMessage("");
setErrorMessage("");
};

const handleCreateOrderClick = (table) => {
const activeSession = getActiveSessionForTable(table.id);

if (activeSession) {
  openOrderForm(table, activeSession);
  return;
}

const confirmCreateSession = globalThis.confirm(
  `Masa ${table.tableNumber} nu are sesiune activa. Doresti sa creezi o sesiune noua pentru aceasta masa?`
);

if (!confirmCreateSession) {
  return;
}

createTableSessionForTable(table.id)
  .then((response) => {
    const newSession = response.data;

    setActiveSessions((currentSessions) => [
      ...currentSessions,
      newSession,
    ]);

    openOrderForm(table, newSession);
  })
  .catch((error) => {
    console.error("Eroare la crearea sesiunii:", error);
    setErrorMessage("Sesiunea pentru masa nu a putut fi creata.");
  });

};

const handleCloseSession = (table, activeSession) => {
const hasActiveOrder = orders.some(
(order) => order.tableSession?.id === activeSession.id
);

if (hasActiveOrder) {
  setOrderMessage("");
  setErrorMessage(
    "Sesiunea nu poate fi inchisa cat timp masa are o comanda activa."
  );
  return;
}

const confirmClose = globalThis.confirm(
  `Doresti sa inchizi sesiunea pentru Masa ${table.tableNumber}?`
);

if (!confirmClose) {
  return;
}

closeTableSession(activeSession.id)
  .then(() => {
    setActiveSessions((currentSessions) =>
      currentSessions.filter(
        (session) => session.id !== activeSession.id
      )
    );

    if (selectedSession?.id === activeSession.id) {
      setSelectedTable(null);
      setSelectedSession(null);
      setProductQuantities({});
    }

    setErrorMessage("");
    setOrderMessage(
      `Sesiunea pentru Masa ${table.tableNumber} a fost inchisa.`
    );
  })
  .catch((error) => {
    console.error("Eroare la inchiderea sesiunii:", error);
    setOrderMessage("");
    setErrorMessage("Sesiunea nu a putut fi inchisa.");
  });

};

const handleQuantityChange = (productId, value) => {
const quantity = Math.max(0, Number(value));

setProductQuantities((currentQuantities) => ({
  ...currentQuantities,
  [productId]: quantity,
}));

};

const handleCancelOrder = () => {
setSelectedTable(null);
setSelectedSession(null);
setProductQuantities({});
setOrderMessage("");
setErrorMessage("");
};

const handleSubmitOrder = () => {
if (!selectedSession?.sessionCode) {
setErrorMessage("Nu exista o sesiune activa pentru masa selectata.");
return;
}

const selectedItems = products
  .filter((product) => Number(productQuantities[product.id] || 0) > 0)
  .map((product) => ({
    productId: product.id,
    quantity: Number(productQuantities[product.id]),
  }));

if (selectedItems.length === 0) {
  setErrorMessage("Selecteaza cel putin un produs.");
  return;
}

const orderRequest = {
  sessionCode: selectedSession.sessionCode,
  items: selectedItems,
};

setSavingOrder(true);
setErrorMessage("");
setOrderMessage("");

createOrder(orderRequest)
  .then(() => {
    setOrderMessage(
      `Comanda pentru Masa ${selectedTable.tableNumber} a fost creata.`
    );

    setSelectedTable(null);
    setSelectedSession(null);
    setProductQuantities({});

    loadOrders();
  })
  .catch((error) => {
    console.error("Eroare la crearea comenzii:", error);
    setErrorMessage("Comanda nu a putut fi creata.");
  })
  .finally(() => {
    setSavingOrder(false);
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

  {orderMessage && <p className="success-message">{orderMessage}</p>}

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
              Stare masa:{" "}
              <strong>
                {activeSession
                  ? "Sesiune activa"
                  : "Fara sesiune activa"}
              </strong>
            </p>

            <button
              className="waiter-button"
              onClick={() => handleCreateOrderClick(table)}
            >
              Creeaza comanda
            </button>

            {activeSession && (
              <button
                className="waiter-button"
                onClick={() =>
                  handleCloseSession(table, activeSession)
                }
              >
                Inchide sesiunea
              </button>
            )}
          </div>
        );
      })}
    </div>
  </section>

  {selectedTable && selectedSession && (
    <section className="waiter-section">
      <h2>Comanda noua pentru Masa {selectedTable.tableNumber}</h2>

      <div className="waiter-grid">
        {products.map((product) => (
          <div key={product.id} className="waiter-card">
            <h3>{product.name}</h3>

            <p>
              Categorie:{" "}
              {product.category?.name || "Categorie necunoscuta"}
            </p>

            <p>Pret: {Number(product.price).toFixed(2)} lei</p>

            <label htmlFor={`quantity-${product.id}`}>
              Cantitate:
            </label>

            <input
              id={`quantity-${product.id}`}
              type="number"
              min="0"
              value={productQuantities[product.id] || 0}
              onChange={(event) =>
                handleQuantityChange(product.id, event.target.value)
              }
            />
          </div>
        ))}
      </div>

      <button
        className="waiter-button"
        onClick={handleSubmitOrder}
        disabled={savingOrder}
      >
        {savingOrder ? "Se salveaza..." : "Salveaza comanda"}
      </button>

      <button
        className="waiter-button"
        onClick={handleCancelOrder}
        disabled={savingOrder}
      >
        Anuleaza
      </button>
    </section>
  )}

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