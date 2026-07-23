import { useEffect, useMemo, useState } from "react";

import { getBarOrders, updateOrderItemStatus } from "../../../../api/orderApi";
import { READY_ITEM_STATUS } from "../constants/barDashboardConstants";
import { getBarItems, getVisibleBarOrders } from "../utils/barDashboardUtils";

function useBarDashboard() {
  const [orders, setOrders] = useState([]);

  const [errorMessage, setErrorMessage] = useState("");

  const loadBarOrders = () => {
    getBarOrders()
      .then((response) => {
        setOrders(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor pentru bar:", error);

        setErrorMessage("Comenzile pentru bar nu au putut fi incarcate.");
      });
  };

  useEffect(() => {
    loadBarOrders();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");

    globalThis.location.href = "/login";
  };

  const handleMarkBarItemsAsReady = (order) => {
    const barItems = getBarItems(order);

    const updateRequests = barItems.map((item) =>
      updateOrderItemStatus(item.id, READY_ITEM_STATUS),
    );

    setErrorMessage("");

    Promise.all(updateRequests)
      .then(() => {
        loadBarOrders();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea bauturilor:", error);

        setErrorMessage("Bauturile nu au putut fi marcate ca gata.");
      });
  };

  const visibleOrders = useMemo(() => getVisibleBarOrders(orders), [orders]);

  return {
    visibleOrders,
    errorMessage,
    handleLogout,
    handleMarkBarItemsAsReady,
  };
}

export default useBarDashboard;
