import { useEffect, useMemo, useState } from "react";

import {
  getActiveOrders,
  getAllOrders,
  getOrderStatistics,
} from "../../../../api/orderApi";
import { getAllFeedback } from "../../../../api/productApi";
import { getTrafficSummary } from "../../../../api/trafficApi";
import {
  getCurrentDateValue,
  getOperationalOrderCounts,
  getProductSalesList,
} from "../utils/managerDashboardUtils";

/*
 * Gestioneaza datele, filtrele si calculele
 * dashboard-ului managerului.
 */
function useManagerDashboard() {
  const initialDate = useMemo(() => getCurrentDateValue(), []);

  const [activeOrders, setActiveOrders] = useState([]);

  const [allOrders, setAllOrders] = useState([]);

  const [feedbackList, setFeedbackList] = useState([]);

  const [statistics, setStatistics] = useState({
    activeOrders: 0,
    servedOrders: 0,
    sales: 0,
    averageRating: 0,
  });

  const [selectedDate, setSelectedDate] = useState(initialDate);

  const [startTime, setStartTime] = useState("00:00");

  const [endTime, setEndTime] = useState("23:59");

  const [appliedFilters, setAppliedFilters] = useState({
    date: initialDate,
    startTime: "00:00",
    endTime: "23:59",
  });

  const [statisticsLoading, setStatisticsLoading] = useState(false);

  const [trafficSummary, setTrafficSummary] = useState({
    entries: 0,
    exits: 0,
    estimatedOccupancy: 0,
  });

  const [errorMessage, setErrorMessage] = useState("");

  const loadStatistics = (date, intervalStart, intervalEnd) => {
    setStatisticsLoading(true);

    return getOrderStatistics(date, intervalStart, intervalEnd)
      .then((response) => {
        setStatistics(response.data);

        setAppliedFilters({
          date,
          startTime: intervalStart,
          endTime: intervalEnd,
        });
      })
      .catch((error) => {
        console.error("Eroare la incarcarea statisticilor:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Statisticile nu au putut fi incarcate.",
        );
      })
      .finally(() => {
        setStatisticsLoading(false);
      });
  };

  const loadManagerData = () => {
    setErrorMessage("");

    loadStatistics(initialDate, "00:00", "23:59");

    getActiveOrders()
      .then((response) => {
        setActiveOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor active:", error);

        setErrorMessage("Comenzile active nu au putut fi incarcate.");
      });

    getAllOrders()
      .then((response) => {
        setAllOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor:", error);

        setErrorMessage("Toate comenzile nu au putut fi incarcate.");
      });

    getAllFeedback()
      .then((response) => {
        setFeedbackList(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea feedback-ului:", error);

        setErrorMessage("Feedback-ul nu a putut fi incarcat.");
      });

    getTrafficSummary()
      .then((response) => {
        setTrafficSummary(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea datelor senzorilor:", error);

        setErrorMessage("Datele senzorilor nu au putut fi incarcate.");
      });
  };

  useEffect(() => {
    loadManagerData();
  }, []);

  const handleStatisticsSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");

    if (!selectedDate || !startTime || !endTime) {
      setErrorMessage("Completeaza data si intervalul orar.");

      return;
    }

    if (endTime < startTime) {
      setErrorMessage("Ora de sfarsit trebuie sa fie dupa ora de inceput.");

      return;
    }

    loadStatistics(selectedDate, startTime, endTime);
  };

  const handleResetStatistics = () => {
    const currentDate = getCurrentDateValue();

    setSelectedDate(currentDate);
    setStartTime("00:00");
    setEndTime("23:59");
    setErrorMessage("");

    loadStatistics(currentDate, "00:00", "23:59");
  };

  const handleSelectedDateChange = (event) => {
    setSelectedDate(event.target.value);
  };

  const handleStartTimeChange = (event) => {
    setStartTime(event.target.value);
  };

  const handleEndTimeChange = (event) => {
    setEndTime(event.target.value);
  };

  const { newOrdersCount, inPreparationOrdersCount, readyOrdersCount } =
    useMemo(() => getOperationalOrderCounts(activeOrders), [activeOrders]);

  const productSalesList = useMemo(
    () => getProductSalesList(allOrders),
    [allOrders],
  );

  return {
    activeOrders,
    feedbackList,
    statistics,
    selectedDate,
    startTime,
    endTime,
    appliedFilters,
    statisticsLoading,
    trafficSummary,
    errorMessage,
    newOrdersCount,
    inPreparationOrdersCount,
    readyOrdersCount,
    productSalesList,
    handleSelectedDateChange,
    handleStartTimeChange,
    handleEndTimeChange,
    handleStatisticsSubmit,
    handleResetStatistics,
  };
}

export default useManagerDashboard;
