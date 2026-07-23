import { useEffect, useMemo, useState } from "react";

import { getAllOrders } from "../../../../api/orderApi";
import { getAllFeedback } from "../../../../api/productApi";
import {
  calculateStatistics,
  getLocalDateValue,
} from "../utils/statisticsUtils";

function createDefaultFilters() {
  const todayValue = getLocalDateValue(new Date());

  return {
    period: "TODAY",
    customStartDate: todayValue,
    customEndDate: todayValue,
    startTime: "00:00",
    endTime: "23:59",
  };
}

/*
 * Gestioneaza datele, filtrele si calculele
 * paginii de statistici a administratorului.
 */
function useAdminStatistics() {
  const initialFilters = useMemo(() => createDefaultFilters(), []);

  const [orders, setOrders] = useState([]);
  const [feedbackList, setFeedbackList] = useState([]);

  const [period, setPeriod] = useState(initialFilters.period);

  const [customStartDate, setCustomStartDate] = useState(
    initialFilters.customStartDate,
  );

  const [customEndDate, setCustomEndDate] = useState(
    initialFilters.customEndDate,
  );

  const [startTime, setStartTime] = useState(initialFilters.startTime);

  const [endTime, setEndTime] = useState(initialFilters.endTime);

  const [appliedFilters, setAppliedFilters] = useState(initialFilters);

  const [loading, setLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let componentActive = true;

    Promise.all([getAllOrders(), getAllFeedback()])
      .then(([ordersResponse, feedbackResponse]) => {
        if (!componentActive) {
          return;
        }

        setOrders(
          Array.isArray(ordersResponse.data) ? ordersResponse.data : [],
        );

        setFeedbackList(
          Array.isArray(feedbackResponse.data) ? feedbackResponse.data : [],
        );
      })
      .catch((error) => {
        if (!componentActive) {
          return;
        }

        console.error("Eroare la incarcarea statisticilor:", error);

        setErrorMessage("Datele pentru statistici nu au putut fi incarcate.");
      })
      .finally(() => {
        if (componentActive) {
          setLoading(false);
        }
      });

    return () => {
      componentActive = false;
    };
  }, []);

  const handlePeriodChange = (event) => {
    setPeriod(event.target.value);
  };

  const handleCustomStartDateChange = (event) => {
    setCustomStartDate(event.target.value);
  };

  const handleCustomEndDateChange = (event) => {
    setCustomEndDate(event.target.value);
  };

  const handleStartTimeChange = (event) => {
    setStartTime(event.target.value);
  };

  const handleEndTimeChange = (event) => {
    setEndTime(event.target.value);
  };

  const handleFilterSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");

    if (period === "CUSTOM" && (!customStartDate || !customEndDate)) {
      setErrorMessage("Selecteaza data de inceput si data de sfarsit.");

      return;
    }

    if (period === "CUSTOM" && customEndDate < customStartDate) {
      setErrorMessage("Data de sfarsit trebuie sa fie dupa data de inceput.");

      return;
    }

    if (endTime < startTime) {
      setErrorMessage("Ora de sfarsit trebuie sa fie dupa ora de inceput.");

      return;
    }

    setAppliedFilters({
      period,
      customStartDate,
      customEndDate,
      startTime,
      endTime,
    });
  };

  const handleResetFilters = () => {
    const defaultFilters = createDefaultFilters();

    setPeriod(defaultFilters.period);

    setCustomStartDate(defaultFilters.customStartDate);

    setCustomEndDate(defaultFilters.customEndDate);

    setStartTime(defaultFilters.startTime);
    setEndTime(defaultFilters.endTime);
    setErrorMessage("");

    setAppliedFilters(defaultFilters);
  };

  const statistics = useMemo(
    () => calculateStatistics(orders, feedbackList, appliedFilters),
    [orders, feedbackList, appliedFilters],
  );

  return {
    period,
    customStartDate,
    customEndDate,
    startTime,
    endTime,
    appliedFilters,
    loading,
    errorMessage,
    statistics,
    handlePeriodChange,
    handleCustomStartDateChange,
    handleCustomEndDateChange,
    handleStartTimeChange,
    handleEndTimeChange,
    handleFilterSubmit,
    handleResetFilters,
  };
}

export default useAdminStatistics;
