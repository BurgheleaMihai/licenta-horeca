import { useEffect, useMemo, useState } from "react";

import { getAllOrders } from "../../../../api/orderApi";
import { initialFilters } from "../constants/orderHistoryConstants";
import {
  calculateDisplayedOrdersValue,
  filterClosedOrders,
  getClosedOrderTableNumbers,
  getDisplayedOrders,
  getSortedClosedOrders,
  validateOrderHistoryFilters,
} from "../utils/orderHistoryUtils";

/*
 * Gestioneaza datele, filtrele si rezultatele
 * istoricului comenzilor.
 */
function useAdminOrderHistory() {
  const [orders, setOrders] = useState([]);

  const [filters, setFilters] = useState(() => ({
    ...initialFilters,
  }));

  const [appliedFilters, setAppliedFilters] = useState(() => ({
    ...initialFilters,
  }));

  /*
   * NONE = nu sunt afisate rezultate
   * FILTERED = sunt afisate rezultatele filtrarii
   * RECENT = sunt afisate ultimele 5 comenzi inchise
   */
  const [resultsMode, setResultsMode] = useState("NONE");

  const [expandedOrderId, setExpandedOrderId] = useState(null);

  const [loading, setLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState("");

  const loadOrders = () => {
    setLoading(true);
    setErrorMessage("");

    getAllOrders()
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea istoricului comenzilor:", error);

        setErrorMessage("Istoricul comenzilor nu a putut fi incarcat.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    loadOrders();
  }, []);

  const tableNumbers = useMemo(
    () => getClosedOrderTableNumbers(orders),
    [orders],
  );

  const sortedClosedOrders = useMemo(
    () => getSortedClosedOrders(orders),
    [orders],
  );

  const filteredOrders = useMemo(
    () => filterClosedOrders(sortedClosedOrders, appliedFilters),
    [sortedClosedOrders, appliedFilters],
  );

  const displayedOrders = useMemo(
    () => getDisplayedOrders(resultsMode, sortedClosedOrders, filteredOrders),
    [resultsMode, sortedClosedOrders, filteredOrders],
  );

  const displayedOrdersValue = useMemo(
    () => calculateDisplayedOrdersValue(displayedOrders),
    [displayedOrders],
  );

  const handleFilterChange = (fieldName, fieldValue) => {
    setFilters((currentFilters) => ({
      ...currentFilters,
      [fieldName]: fieldValue,
    }));
  };

  const handleFilterSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");
    setExpandedOrderId(null);

    const validationError = validateOrderHistoryFilters(filters);

    if (validationError) {
      setErrorMessage(validationError);

      return;
    }

    setAppliedFilters({
      ...filters,
    });

    setResultsMode("FILTERED");
  };

  const handleShowRecentOrders = () => {
    setErrorMessage("");
    setExpandedOrderId(null);
    setResultsMode("RECENT");
  };

  const handleResetFilters = () => {
    setFilters({
      ...initialFilters,
    });

    setAppliedFilters({
      ...initialFilters,
    });

    setExpandedOrderId(null);
    setResultsMode("NONE");
    setErrorMessage("");
  };

  const handleToggleDetails = (orderId) => {
    setExpandedOrderId((currentOrderId) =>
      currentOrderId === orderId ? null : orderId,
    );
  };

  return {
    filters,
    resultsMode,
    expandedOrderId,
    loading,
    errorMessage,
    tableNumbers,
    displayedOrders,
    displayedOrdersValue,
    loadOrders,
    handleFilterChange,
    handleFilterSubmit,
    handleShowRecentOrders,
    handleResetFilters,
    handleToggleDetails,
  };
}

export default useAdminOrderHistory;
