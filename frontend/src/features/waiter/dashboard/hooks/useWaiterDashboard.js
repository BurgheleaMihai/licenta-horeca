import { useEffect, useState } from "react";

import {
  createOrder,
  getActiveOrders,
  updateOrderStatus,
} from "../../../../api/orderApi";
import { getAvailableProducts } from "../../../../api/productApi";
import { getAllTables } from "../../../../api/tableApi";
import {
  closeTableSession,
  createTableSessionForTable,
  getActiveTableSessions,
} from "../../../../api/tableSessionApi";
import { ORDER_STATUS } from "../constants/waiterDashboardConstants";
import {
  buildSelectedOrderItems,
  findActiveSessionForTable,
  getServerErrorMessage,
} from "../utils/waiterDashboardUtils";

function useWaiterDashboard() {
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
        setTables(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea meselor:", error);

        setErrorMessage("Mesele nu au putut fi incarcate.");
      });
  };

  const loadOrders = () => {
    getActiveOrders()
      .then((response) => {
        setOrders(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor:", error);

        setErrorMessage("Comenzile nu au putut fi incarcate.");
      });
  };

  const loadActiveSessions = () => {
    getActiveTableSessions()
      .then((response) => {
        setActiveSessions(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea meselor deschise:", error);

        setErrorMessage("Mesele deschise nu au putut fi incarcate.");
      });
  };

  const loadProducts = () => {
    getAvailableProducts()
      .then((response) => {
        setProducts(Array.isArray(response.data) ? response.data : []);
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

  const handleOpenQrPage = () => {
    globalThis.open("/menu-qr", "_blank", "noopener,noreferrer");
  };

  const openOrderForm = (table, session) => {
    setSelectedTable(table);
    setSelectedSession(session);
    setProductQuantities({});
    setOrderMessage("");
    setErrorMessage("");
  };

  const handleOpenSession = (table) => {
    setOrderMessage("");
    setErrorMessage("");

    createTableSessionForTable(table.id)
      .then((response) => {
        const newSession = response.data;

        setActiveSessions((currentSessions) => [
          ...currentSessions.filter(
            (session) => session.restaurantTable?.id !== table.id,
          ),
          newSession,
        ]);

        setOrderMessage(`Masa ${table.tableNumber} a fost deschisa.`);
      })
      .catch((error) => {
        console.error("Eroare la deschiderea mesei:", error);

        setOrderMessage("");

        setErrorMessage(
          getServerErrorMessage(error, "Masa nu a putut fi deschisa."),
        );

        loadActiveSessions();
      });
  };

  const handleCreateOrderClick = (table) => {
    const activeSession = findActiveSessionForTable(activeSessions, table.id);

    if (!activeSession) {
      setOrderMessage("");

      setErrorMessage(`Deschide mai intai Masa ${table.tableNumber}.`);

      return;
    }

    openOrderForm(table, activeSession);
  };

  const handleCloseSession = (table, activeSession) => {
    const hasActiveOrder = orders.some(
      (order) => order.tableSession?.id === activeSession.id,
    );

    if (hasActiveOrder) {
      setOrderMessage("");

      setErrorMessage(
        "Masa nu poate fi inchisa cat timp are o comanda activa.",
      );

      return;
    }

    const confirmClose = globalThis.confirm(
      `Confirmi ca Masa ${table.tableNumber} a fost platita si poate fi inchisa?`,
    );

    if (!confirmClose) {
      return;
    }

    closeTableSession(activeSession.id)
      .then(() => {
        setActiveSessions((currentSessions) =>
          currentSessions.filter((session) => session.id !== activeSession.id),
        );

        if (selectedSession?.id === activeSession.id) {
          setSelectedTable(null);
          setSelectedSession(null);
          setProductQuantities({});
        }

        setErrorMessage("");

        setOrderMessage(`Masa ${table.tableNumber} a fost inchisa.`);
      })
      .catch((error) => {
        console.error("Eroare la inchiderea mesei:", error);

        setOrderMessage("");

        setErrorMessage(
          getServerErrorMessage(error, "Masa nu a putut fi inchisa."),
        );

        loadActiveSessions();
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
      setErrorMessage("Nu exista o masa deschisa pentru comanda selectata.");

      return;
    }

    const selectedItems = buildSelectedOrderItems(products, productQuantities);

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
          `Comanda pentru Masa ${selectedTable.tableNumber} a fost creata.`,
        );

        setSelectedTable(null);
        setSelectedSession(null);
        setProductQuantities({});

        loadOrders();
      })
      .catch((error) => {
        console.error("Eroare la crearea comenzii:", error);

        setErrorMessage(
          getServerErrorMessage(error, "Comanda nu a putut fi creata."),
        );
      })
      .finally(() => {
        setSavingOrder(false);
      });
  };

  const handleMarkOrderAsServed = (orderId) => {
    updateOrderStatus(orderId, ORDER_STATUS.SERVED)
      .then(() => {
        loadOrders();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea statusului:", error);

        setErrorMessage("Statusul comenzii nu a putut fi actualizat.");
      });
  };

  const handleSendOrderToPreparation = (orderId) => {
    updateOrderStatus(orderId, ORDER_STATUS.IN_PREPARATION)
      .then(() => {
        loadOrders();
      })
      .catch((error) => {
        console.error("Eroare la trimiterea comenzii la preparare:", error);

        setErrorMessage("Comanda nu a putut fi trimisa la preparare.");
      });
  };

  return {
    tables,
    orders,
    activeSessions,
    products,
    selectedTable,
    selectedSession,
    productQuantities,
    errorMessage,
    orderMessage,
    savingOrder,
    handleLogout,
    handleOpenQrPage,
    handleOpenSession,
    handleCreateOrderClick,
    handleCloseSession,
    handleQuantityChange,
    handleCancelOrder,
    handleSubmitOrder,
    handleMarkOrderAsServed,
    handleSendOrderToPreparation,
  };
}

export default useWaiterDashboard;
