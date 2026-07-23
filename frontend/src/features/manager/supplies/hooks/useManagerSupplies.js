import { useEffect, useMemo, useState } from "react";

import {
  addStockEntry,
  deleteStockEntry,
  getAllActiveAuxiliarySupplies,
  getStockEntries,
  markSupplyAvailable,
  markSupplyUnavailable,
  updateStockEntry,
} from "../../../../api/auxiliarySupplyApi";
import {
  INITIAL_ENTRY_FORM,
  UNIT_LABELS,
} from "../constants/managerSuppliesConstants";
import {
  buildEntryData,
  buildInitialVariantSelections,
  createEntryFormForSupply,
  findTargetSupply,
  getCompatibleInputUnits,
  getEntryFormValidationError,
  groupSupplies,
} from "../utils/managerSuppliesUtils";

function useManagerSupplies() {
  const [supplies, setSupplies] = useState([]);

  const [selectedVariantIds, setSelectedVariantIds] = useState({});

  const [selectedSupply, setSelectedSupply] = useState(null);

  const [entryForm, setEntryForm] = useState({
    ...INITIAL_ENTRY_FORM,
  });

  const [editingEntryId, setEditingEntryId] = useState(null);

  const [entryHistory, setEntryHistory] = useState([]);

  const [loadingHistory, setLoadingHistory] = useState(false);

  const [savingEntry, setSavingEntry] = useState(false);

  const [deletingEntryId, setDeletingEntryId] = useState(null);

  const [errorMessage, setErrorMessage] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  const groupedSupplies = useMemo(() => groupSupplies(supplies), [supplies]);

  const targetSupply = useMemo(
    () => findTargetSupply(supplies, entryForm.supplyId, selectedSupply),
    [supplies, entryForm.supplyId, selectedSupply],
  );

  const compatibleInputUnits = useMemo(
    () => getCompatibleInputUnits(targetSupply),
    [targetSupply],
  );

  const resetEntryForm = (supply) => {
    setEntryForm(createEntryFormForSupply(supply));

    setEditingEntryId(null);
  };

  const loadEntryHistory = (supplyId) => {
    setLoadingHistory(true);

    getStockEntries(supplyId)
      .then((response) => {
        setEntryHistory(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea istoricului:", error);

        setErrorMessage("Istoricul intrarilor nu a putut fi incarcat.");
      })
      .finally(() => {
        setLoadingHistory(false);
      });
  };

  const loadSupplies = () => {
    setErrorMessage("");

    getAllActiveAuxiliarySupplies()
      .then((response) => {
        const loadedSupplies = Array.isArray(response.data)
          ? response.data
          : [];

        setSupplies(loadedSupplies);

        const initialSelections = buildInitialVariantSelections(loadedSupplies);

        setSelectedVariantIds((currentSelections) => ({
          ...initialSelections,
          ...currentSelections,
        }));

        if (selectedSupply) {
          const updatedSupply = loadedSupplies.find(
            (supply) => supply.id === selectedSupply.id,
          );

          if (updatedSupply) {
            setSelectedSupply(updatedSupply);
          }
        }
      })
      .catch((error) => {
        console.error("Eroare la incarcarea stocurilor:", error);

        setErrorMessage("Stocurile nu au putut fi incarcate.");
      });
  };

  useEffect(() => {
    loadSupplies();
  }, []);

  const handleVariantSelectionChange = (groupKey, event) => {
    const selectedId = event.target.value;

    setSelectedVariantIds((currentSelections) => ({
      ...currentSelections,
      [groupKey]: selectedId,
    }));
  };

  const handleSelectSupply = (supply) => {
    setSelectedSupply(supply);
    setEntryHistory([]);
    setErrorMessage("");
    setSuccessMessage("");

    resetEntryForm(supply);
    loadEntryHistory(supply.id);

    globalThis.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleEntryFormChange = (event) => {
    const { name, value } = event.target;

    setEntryForm((currentForm) => ({
      ...currentForm,
      [name]: value,
    }));
  };

  const handleTargetSupplyChange = (event) => {
    const newSupplyId = event.target.value;

    const newSupply = supplies.find(
      (supply) => supply.id === Number(newSupplyId),
    );

    setEntryForm((currentForm) => ({
      ...currentForm,
      supplyId: newSupplyId,

      inputUnit: newSupply?.baseUnit || currentForm.inputUnit,
    }));
  };

  const handlePackageTypeChange = (event) => {
    const newPackageType = event.target.value;

    setEntryForm((currentForm) => ({
      ...currentForm,

      packageType: newPackageType,

      quantityPerPackage:
        newPackageType === "DIRECT" ? "1" : currentForm.quantityPerPackage,
    }));
  };

  const handleSaveStockEntry = (event) => {
    event.preventDefault();

    if (!selectedSupply) {
      setErrorMessage("Selecteaza o varianta de stoc.");

      return;
    }

    setErrorMessage("");
    setSuccessMessage("");

    const validationError = getEntryFormValidationError(
      entryForm,
      editingEntryId,
    );

    if (validationError) {
      setErrorMessage(validationError);

      return;
    }

    const entryData = buildEntryData(entryForm, selectedSupply);

    setSavingEntry(true);

    const request = editingEntryId
      ? updateStockEntry(editingEntryId, entryData)
      : addStockEntry(selectedSupply.id, entryData);

    request
      .then((response) => {
        const entry = response.data;

        if (editingEntryId) {
          setSuccessMessage(
            "Intrarea a fost modificata, iar stocurile au fost recalculate.",
          );
        } else {
          setSuccessMessage(
            `Au fost adaugate ${entry.convertedQuantity} ` +
              `${UNIT_LABELS[selectedSupply.baseUnit]}. ` +
              `Stocul nou este ${entry.newQuantity} ` +
              `${UNIT_LABELS[selectedSupply.baseUnit]}.`,
          );
        }

        resetEntryForm(selectedSupply);
        loadSupplies();

        loadEntryHistory(selectedSupply.id);
      })
      .catch((error) => {
        console.error("Eroare la salvarea intrarii:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Intrarea de marfa nu a putut fi salvata.",
        );
      })
      .finally(() => {
        setSavingEntry(false);
      });
  };

  const handleEditEntry = (entry) => {
    setEditingEntryId(entry.id);
    setErrorMessage("");
    setSuccessMessage("");

    setEntryForm({
      supplyId: String(entry.supply?.id || selectedSupply.id),

      packageQuantity: String(entry.packageQuantity ?? 1),

      packageType: entry.packageType || "DIRECT",

      quantityPerPackage: String(entry.quantityPerPackage ?? 1),

      inputUnit: entry.inputUnit || selectedSupply.baseUnit,

      notes: entry.notes || "",
    });

    globalThis.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleCancelEntryEdit = () => {
    resetEntryForm(selectedSupply);
    setErrorMessage("");
    setSuccessMessage("");
  };

  const handleDeleteEntry = (entry) => {
    const confirmed = globalThis.confirm(
      "Sigur doresti sa stergi aceasta intrare? " +
        "Cantitatea adaugata prin ea va fi scazuta din stoc.",
    );

    if (!confirmed) {
      return;
    }

    setErrorMessage("");
    setSuccessMessage("");
    setDeletingEntryId(entry.id);

    deleteStockEntry(entry.id)
      .then(() => {
        setSuccessMessage(
          "Intrarea a fost stearsa, iar stocul a fost recalculat.",
        );

        if (editingEntryId === entry.id) {
          resetEntryForm(selectedSupply);
        }

        loadSupplies();

        loadEntryHistory(selectedSupply.id);
      })
      .catch((error) => {
        console.error("Eroare la stergerea intrarii:", error);

        setErrorMessage(
          error.response?.data?.message || "Intrarea nu a putut fi stearsa.",
        );
      })
      .finally(() => {
        setDeletingEntryId(null);
      });
  };

  const handleMarkUnavailable = (supplyId) => {
    setErrorMessage("");
    setSuccessMessage("");

    markSupplyUnavailable(supplyId)
      .then(() => {
        setSuccessMessage("Lipsa a fost semnalata pentru varianta selectata.");

        loadSupplies();
      })
      .catch((error) => {
        console.error("Eroare la semnalarea lipsei:", error);

        setErrorMessage("Lipsa nu a putut fi semnalata.");
      });
  };

  const handleMarkAvailable = (supplyId) => {
    setErrorMessage("");
    setSuccessMessage("");

    markSupplyAvailable(supplyId)
      .then(() => {
        setSuccessMessage("Varianta a fost marcata disponibila.");

        loadSupplies();
      })
      .catch((error) => {
        console.error("Eroare la actualizarea variantei:", error);

        setErrorMessage("Varianta nu a putut fi marcata disponibila.");
      });
  };

  const handleBackToManager = () => {
    globalThis.location.href = "/manager";
  };

  const handleCloseEntryForm = () => {
    setSelectedSupply(null);
    setEntryHistory([]);
    setEditingEntryId(null);
    setErrorMessage("");
    setSuccessMessage("");

    setEntryForm({
      ...INITIAL_ENTRY_FORM,
    });
  };

  return {
    supplies,
    groupedSupplies,
    selectedVariantIds,
    selectedSupply,
    entryForm,
    editingEntryId,
    entryHistory,
    loadingHistory,
    savingEntry,
    deletingEntryId,
    errorMessage,
    successMessage,
    compatibleInputUnits,
    handleVariantSelectionChange,
    handleSelectSupply,
    handleEntryFormChange,
    handleTargetSupplyChange,
    handlePackageTypeChange,
    handleSaveStockEntry,
    handleEditEntry,
    handleCancelEntryEdit,
    handleDeleteEntry,
    handleMarkUnavailable,
    handleMarkAvailable,
    handleBackToManager,
    handleCloseEntryForm,
  };
}

export default useManagerSupplies;
