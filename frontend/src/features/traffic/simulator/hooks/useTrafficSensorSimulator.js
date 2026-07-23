import { useEffect, useState } from "react";

import {
  getTrafficSummary,
  registerEntry,
  registerExit,
} from "../../../../api/trafficApi";
import { INITIAL_TRAFFIC_SUMMARY } from "../constants/trafficSimulatorConstants";
import { normalizeTrafficSummary } from "../utils/trafficSimulatorUtils";

function useTrafficSensorSimulator() {
  const [trafficSummary, setTrafficSummary] = useState({
    ...INITIAL_TRAFFIC_SUMMARY,
  });

  const loadTrafficSummary = () => {
    return getTrafficSummary()
      .then((response) => {
        setTrafficSummary(normalizeTrafficSummary(response.data));
      })
      .catch((error) => {
        console.error("Eroare la incarcarea datelor senzorilor:", error);
      });
  };

  useEffect(() => {
    loadTrafficSummary();
  }, []);

  const handleSimulateEntry = () => {
    registerEntry()
      .then(() => {
        loadTrafficSummary();
      })
      .catch((error) => {
        console.error(
          "Evenimentul de intrare nu a putut fi inregistrat:",
          error,
        );
      });
  };

  const handleSimulateExit = () => {
    registerExit()
      .then(() => {
        loadTrafficSummary();
      })
      .catch((error) => {
        console.error(
          "Evenimentul de iesire nu a putut fi inregistrat:",
          error,
        );
      });
  };

  return {
    trafficSummary,
    handleSimulateEntry,
    handleSimulateExit,
  };
}

export default useTrafficSensorSimulator;
