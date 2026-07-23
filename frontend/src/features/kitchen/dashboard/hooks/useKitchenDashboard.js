import { useEffect, useMemo, useState } from "react";

import {
  getKitchenOrders,
  updateOrderItemStatus,
} from "../../../../api/orderApi";
import { READY_ITEM_STATUS } from "../constants/kitchenDashboardConstants";
import {
  getKitchenItems,
  getVisibleKitchenOrders,
} from "../utils/kitchenDashboardUtils";

function useKitchenDashboard() {
  const [orders, setOrders] = useState([]);

  const [errorMessage, setErrorMessage] = useState("");

  const loadKitchenOrders = () => {
    getKitchenOrders()
      .then((response) => {
        setOrders(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error(
          "Eroare la incarcarea comenzilor pentru bucatarie:",
          error,
        );

        setErrorMessage("Comenzile pentru bucatarie nu au putut fi incarcate.");
      });
  };

  useEffect(() => {
    loadKitchenOrders();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");

    globalThis.location.href = "/login";
  };

  const handleMarkKitchenItemsAsReady = (order) => {
    const kitchenItems = getKitchenItems(order);

    const updateRequests = kitchenItems.map((item) =>
      updateOrderItemStatus(item.id, READY_ITEM_STATUS),
    );

    setErrorMessage("");

    Promise.all(updateRequests)
      .then(() => {
        loadKitchenOrders();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea produselor de bucatarie:", error);

        setErrorMessage(
          "Produsele de bucatarie nu au putut fi marcate ca gata.",
        );
      });
  };

  const visibleOrders = useMemo(
    () => getVisibleKitchenOrders(orders),
    [orders],
  );

  return {
    visibleOrders,
    errorMessage,
    handleLogout,
    handleMarkKitchenItemsAsReady,
  };
}

export default useKitchenDashboard;
