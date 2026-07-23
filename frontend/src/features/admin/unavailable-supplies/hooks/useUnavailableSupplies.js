import { useEffect, useState } from "react";

import { getUnavailableAuxiliarySupplies } from "../../../../api/auxiliarySupplyApi";

/*
 * Gestioneaza incarcarea articolelor de stoc
 * semnalate ca indisponibile.
 */
function useUnavailableSupplies() {
  const [unavailableSupplies, setUnavailableSupplies] = useState([]);

  const [loading, setLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState("");

  const loadUnavailableSupplies = () => {
    setLoading(true);
    setErrorMessage("");

    getUnavailableAuxiliarySupplies()
      .then((response) => {
        setUnavailableSupplies(
          Array.isArray(response.data) ? response.data : [],
        );
      })
      .catch((error) => {
        console.error("Eroare la incarcarea articolelor lipsa:", error);

        setErrorMessage("Articolele de stoc lipsa nu au putut fi incarcate.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    loadUnavailableSupplies();
  }, []);

  return {
    unavailableSupplies,
    loading,
    errorMessage,
    loadUnavailableSupplies,
  };
}

export default useUnavailableSupplies;
