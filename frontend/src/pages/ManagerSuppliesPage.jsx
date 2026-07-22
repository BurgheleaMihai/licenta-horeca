import { useEffect, useMemo, useState } from "react";
import {
  addStockEntry,
  deleteStockEntry,
  getAllActiveAuxiliarySupplies,
  getStockEntries,
  markSupplyAvailable,
  markSupplyUnavailable,
  updateStockEntry,
} from "../api/auxiliarySupplyApi";

const stockTypeLabels = {
  AUXILIARY: "Zona 1 - Auxiliar",
  WAREHOUSE: "Zona 2 - Depozit",
  FRUIT_AND_VEGETABLE: "Zona 3 - Fructe si legume",
};

const categoryLabels = {
  PACKAGING: "Ambalaje",
  CONSUMABLE: "Consumabile",
  BEVERAGE_INGREDIENT: "Ingrediente pentru bauturi",
  MEAT: "Carne",
  DAIRY: "Lactate",
  DRY_PRODUCT: "Produse uscate",
  FROZEN_PRODUCT: "Produse congelate",
  SAUCE: "Sosuri",
  OTHER_INGREDIENT: "Alte ingrediente",
  FRUIT: "Fructe",
  VEGETABLE: "Legume",
  OTHER: "Altele",
};

const unitLabels = {
  PIECE: "buc",
  GRAM: "g",
  KILOGRAM: "kg",
  MILLILITER: "ml",
  LITER: "l",
};

const packageTypeLabels = {
  DIRECT: "Cantitate directa",
  BOX: "Cutie",
  PACK: "Pachet",
  BUNDLE: "Bax",
  BAG: "Sac",
  BOTTLE: "Sticla",
};

const stockTypes = ["AUXILIARY", "WAREHOUSE", "FRUIT_AND_VEGETABLE"];

const packageTypes = ["DIRECT", "BOX", "PACK", "BUNDLE", "BAG", "BOTTLE"];

const allMeasurementUnits = [
  "PIECE",
  "GRAM",
  "KILOGRAM",
  "MILLILITER",
  "LITER",
];

const initialEntryForm = {
  supplyId: "",
  packageQuantity: "1",
  packageType: "DIRECT",
  quantityPerPackage: "1",
  inputUnit: "PIECE",
  notes: "",
};

function ManagerSuppliesPage() {
  const [supplies, setSupplies] = useState([]);

  const [selectedVariantIds, setSelectedVariantIds] = useState({});

  const [selectedSupply, setSelectedSupply] = useState(null);

  const [entryForm, setEntryForm] = useState(initialEntryForm);

  const [editingEntryId, setEditingEntryId] = useState(null);

  const [entryHistory, setEntryHistory] = useState([]);

  const [loadingHistory, setLoadingHistory] = useState(false);

  const [savingEntry, setSavingEntry] = useState(false);

  const [deletingEntryId, setDeletingEntryId] = useState(null);

  const [errorMessage, setErrorMessage] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    loadSupplies();
  }, []);

  const groupedSupplies = useMemo(() => {
    const groups = {};

    supplies.forEach((supply) => {
      const groupKey = `${supply.stockType}-${supply.name}`;

      if (!groups[groupKey]) {
        groups[groupKey] = {
          key: groupKey,
          name: supply.name,
          stockType: supply.stockType,
          category: supply.category,
          variants: [],
        };
      }

      groups[groupKey].variants.push(supply);
    });

    return Object.values(groups)
      .map((group) => ({
        ...group,
        variants: [...group.variants].sort((firstVariant, secondVariant) => {
          const firstName = firstVariant.variantName || "Varianta unica";

          const secondName = secondVariant.variantName || "Varianta unica";

          return firstName.localeCompare(secondName, "ro", {
            numeric: true,
            sensitivity: "base",
          });
        }),
      }))
      .sort((firstGroup, secondGroup) =>
        firstGroup.name.localeCompare(secondGroup.name, "ro", {
          sensitivity: "base",
        }),
      );
  }, [supplies]);

  const targetSupply = useMemo(() => {
    if (!entryForm.supplyId) {
      return selectedSupply;
    }

    return (
      supplies.find((supply) => supply.id === Number(entryForm.supplyId)) ||
      selectedSupply
    );
  }, [entryForm.supplyId, selectedSupply, supplies]);

  const compatibleInputUnits = useMemo(() => {
    if (!targetSupply) {
      return allMeasurementUnits;
    }

    if (
      targetSupply.baseUnit === "GRAM" ||
      targetSupply.baseUnit === "KILOGRAM"
    ) {
      return ["GRAM", "KILOGRAM"];
    }

    if (
      targetSupply.baseUnit === "MILLILITER" ||
      targetSupply.baseUnit === "LITER"
    ) {
      return ["MILLILITER", "LITER"];
    }

    return ["PIECE"];
  }, [targetSupply]);

  const initializeSelectedVariants = (loadedSupplies) => {
    const initialSelections = {};

    loadedSupplies.forEach((supply) => {
      const groupKey = `${supply.stockType}-${supply.name}`;

      if (!initialSelections[groupKey]) {
        initialSelections[groupKey] = String(supply.id);
      }
    });

    setSelectedVariantIds((currentSelections) => ({
      ...initialSelections,
      ...currentSelections,
    }));
  };

  const loadSupplies = () => {
    setErrorMessage("");

    getAllActiveAuxiliarySupplies()
      .then((response) => {
        const loadedSupplies = response.data;

        setSupplies(loadedSupplies);

        initializeSelectedVariants(loadedSupplies);

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

  const loadEntryHistory = (supplyId) => {
    setLoadingHistory(true);

    getStockEntries(supplyId)
      .then((response) => {
        setEntryHistory(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea istoricului:", error);

        setErrorMessage("Istoricul intrarilor nu a putut fi incarcat.");
      })
      .finally(() => {
        setLoadingHistory(false);
      });
  };

  const getSelectedVariantForGroup = (group) => {
    const selectedId = Number(selectedVariantIds[group.key]);

    return (
      group.variants.find((variant) => variant.id === selectedId) ||
      group.variants[0]
    );
  };

  const getCompleteSupplyName = (supply) => {
    if (!supply) {
      return "";
    }

    if (!supply.variantName) {
      return supply.name;
    }

    return `${supply.name} - ${supply.variantName}`;
  };

  const resetEntryForm = (supply) => {
    setEntryForm({
      ...initialEntryForm,
      supplyId: supply ? String(supply.id) : "",
      inputUnit: supply?.baseUnit || "PIECE",
    });

    setEditingEntryId(null);
  };

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

  const validateEntryForm = () => {
    const packageQuantity = Number(entryForm.packageQuantity);

    const quantityPerPackage = Number(entryForm.quantityPerPackage);

    if (Number.isNaN(packageQuantity) || packageQuantity <= 0) {
      setErrorMessage("Cantitatea primita trebuie sa fie mai mare decat zero.");

      return false;
    }

    if (
      entryForm.packageType !== "DIRECT" &&
      !Number.isInteger(packageQuantity)
    ) {
      setErrorMessage("Numarul de ambalaje trebuie sa fie un numar intreg.");

      return false;
    }

    if (
      entryForm.packageType !== "DIRECT" &&
      (Number.isNaN(quantityPerPackage) || quantityPerPackage <= 0)
    ) {
      setErrorMessage(
        "Cantitatea dintr-un ambalaj trebuie sa fie mai mare decat zero.",
      );

      return false;
    }

    if (editingEntryId && !entryForm.supplyId) {
      setErrorMessage("Varianta destinatie este obligatorie.");

      return false;
    }

    return true;
  };

  const buildEntryData = () => {
    return {
      supplyId: entryForm.supplyId
        ? Number(entryForm.supplyId)
        : selectedSupply.id,

      packageQuantity: Number(entryForm.packageQuantity),

      packageType: entryForm.packageType,

      quantityPerPackage:
        entryForm.packageType === "DIRECT"
          ? 1
          : Number(entryForm.quantityPerPackage),

      inputUnit: entryForm.inputUnit,

      notes: entryForm.notes.trim(),
    };
  };

  const handleSaveStockEntry = (event) => {
    event.preventDefault();

    if (!selectedSupply) {
      setErrorMessage("Selecteaza o varianta de stoc.");

      return;
    }

    setErrorMessage("");
    setSuccessMessage("");

    if (!validateEntryForm()) {
      return;
    }

    const entryData = buildEntryData();

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
              `${unitLabels[selectedSupply.baseUnit]}. ` +
              `Stocul nou este ${entry.newQuantity} ` +
              `${unitLabels[selectedSupply.baseUnit]}.`,
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
    setEntryForm(initialEntryForm);
  };

  const formatDate = (dateValue) => {
    if (!dateValue) {
      return "Data necunoscuta";
    }

    return new Date(dateValue).toLocaleString("ro-RO");
  };

  return (
    <div className="manager-page">
      <header className="manager-header">
        <h1>Stocuri restaurant</h1>

        <button type="button" onClick={handleBackToManager}>
          Inapoi la panoul managerului
        </button>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      {successMessage && <p className="feedback-message">{successMessage}</p>}

      {selectedSupply && (
        <section className="manager-section">
          <h2>
            {editingEntryId ? "Modifica intrarea" : "Intrare marfa"} -{" "}
            {getCompleteSupplyName(selectedSupply)}
          </h2>

          <div className="stock-entry-summary">
            <p>
              Produs: <strong>{selectedSupply.name}</strong>
            </p>

            <p>
              Varianta deschisa:{" "}
              <strong>{selectedSupply.variantName || "Varianta unica"}</strong>
            </p>

            <p>
              Stoc curent:{" "}
              <strong>
                {selectedSupply.currentQuantity}{" "}
                {unitLabels[selectedSupply.baseUnit]}
              </strong>
            </p>
          </div>

          <form className="stock-entry-form" onSubmit={handleSaveStockEntry}>
            <div className="stock-entry-form-grid">
              {editingEntryId && (
                <div className="filter-group">
                  <label htmlFor="entry-supply">Produs si varianta</label>

                  <select
                    id="entry-supply"
                    name="supplyId"
                    value={entryForm.supplyId}
                    onChange={handleTargetSupplyChange}
                  >
                    {supplies.map((supply) => (
                      <option key={supply.id} value={supply.id}>
                        {getCompleteSupplyName(supply)}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <div className="filter-group">
                <label htmlFor="package-type">Tipul intrarii</label>

                <select
                  id="package-type"
                  name="packageType"
                  value={entryForm.packageType}
                  onChange={handlePackageTypeChange}
                >
                  {packageTypes.map((packageType) => (
                    <option key={packageType} value={packageType}>
                      {packageTypeLabels[packageType]}
                    </option>
                  ))}
                </select>
              </div>

              <div className="filter-group">
                <label htmlFor="package-quantity">
                  {entryForm.packageType === "DIRECT"
                    ? "Cantitate primita"
                    : "Numar de ambalaje"}
                </label>

                <input
                  id="package-quantity"
                  name="packageQuantity"
                  type="number"
                  min={entryForm.packageType === "DIRECT" ? "0.001" : "1"}
                  step={entryForm.packageType === "DIRECT" ? "0.001" : "1"}
                  value={entryForm.packageQuantity}
                  onChange={handleEntryFormChange}
                />
              </div>

              {entryForm.packageType !== "DIRECT" && (
                <div className="filter-group">
                  <label htmlFor="quantity-per-package">
                    Cantitate intr-un ambalaj
                  </label>

                  <input
                    id="quantity-per-package"
                    name="quantityPerPackage"
                    type="number"
                    min="0.001"
                    step="0.001"
                    value={entryForm.quantityPerPackage}
                    onChange={handleEntryFormChange}
                  />
                </div>
              )}

              <div className="filter-group">
                <label htmlFor="input-unit">Unitatea de pe ambalaj</label>

                <select
                  id="input-unit"
                  name="inputUnit"
                  value={entryForm.inputUnit}
                  onChange={handleEntryFormChange}
                >
                  {compatibleInputUnits.map((unit) => (
                    <option key={unit} value={unit}>
                      {unitLabels[unit]}
                    </option>
                  ))}
                </select>
              </div>

              <div className="filter-group">
                <label htmlFor="entry-notes">Observatii</label>

                <input
                  id="entry-notes"
                  name="notes"
                  type="text"
                  value={entryForm.notes}
                  onChange={handleEntryFormChange}
                  placeholder="Optional"
                />
              </div>
            </div>

            <div className="stock-entry-actions">
              <button type="submit" disabled={savingEntry}>
                {savingEntry
                  ? "Se salveaza..."
                  : editingEntryId
                    ? "Salveaza modificarea"
                    : "Adauga intrarea"}
              </button>

              {editingEntryId && (
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleCancelEntryEdit}
                >
                  Renunta la modificare
                </button>
              )}

              <button
                type="button"
                className="secondary-button"
                onClick={handleCloseEntryForm}
              >
                Inchide
              </button>
            </div>
          </form>

          <div className="stock-entry-history">
            <h3>Istoric intrari</h3>

            {loadingHistory ? (
              <p>Se incarca istoricul...</p>
            ) : entryHistory.length === 0 ? (
              <p>Nu exista intrari salvate pentru aceasta varianta.</p>
            ) : (
              <div className="manager-grid">
                {entryHistory.map((entry) => (
                  <article key={entry.id} className="manager-card">
                    <h4>{packageTypeLabels[entry.packageType]}</h4>

                    <p>
                      Varianta:{" "}
                      <strong>{getCompleteSupplyName(entry.supply)}</strong>
                    </p>

                    <p>Ambalaje/cantitate: {entry.packageQuantity}</p>

                    {entry.packageType !== "DIRECT" && (
                      <p>
                        Cantitate per ambalaj: {entry.quantityPerPackage}{" "}
                        {unitLabels[entry.inputUnit]}
                      </p>
                    )}

                    <p>
                      Cantitate adaugata:{" "}
                      <strong>
                        {entry.convertedQuantity}{" "}
                        {unitLabels[entry.supply?.baseUnit]}
                      </strong>
                    </p>

                    <p>Stoc anterior: {entry.previousQuantity}</p>

                    <p>Stoc nou: {entry.newQuantity}</p>

                    <p>Data: {formatDate(entry.createdAt)}</p>

                    {entry.notes && <p>Observatii: {entry.notes}</p>}

                    <div className="stock-card-actions">
                      <button
                        type="button"
                        onClick={() => handleEditEntry(entry)}
                      >
                        Modifica
                      </button>

                      <button
                        type="button"
                        className="danger-button"
                        disabled={deletingEntryId === entry.id}
                        onClick={() => handleDeleteEntry(entry)}
                      >
                        {deletingEntryId === entry.id
                          ? "Se sterge..."
                          : "Sterge"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {stockTypes.map((stockType) => {
        const groupsFromZone = groupedSupplies.filter(
          (group) => group.stockType === stockType,
        );

        return (
          <section key={stockType} className="manager-section">
            <h2>{stockTypeLabels[stockType]}</h2>

            {groupsFromZone.length === 0 ? (
              <p>Nu exista produse configurate in aceasta zona.</p>
            ) : (
              <div className="manager-grid">
                {groupsFromZone.map((group) => {
                  const selectedVariant = getSelectedVariantForGroup(group);

                  const belowMinimum =
                    Number(selectedVariant.currentQuantity) <
                    Number(selectedVariant.minimumQuantity);

                  return (
                    <article
                      key={group.key}
                      className={
                        belowMinimum || !selectedVariant.availableInWarehouse
                          ? "manager-card stock-warning-card"
                          : "manager-card"
                      }
                    >
                      <h3>{group.name}</h3>

                      <p>
                        Categorie:{" "}
                        {categoryLabels[group.category] || group.category}
                      </p>

                      <div className="filter-group">
                        <label htmlFor={`variant-${group.key}`}>Varianta</label>

                        <select
                          id={`variant-${group.key}`}
                          value={
                            selectedVariantIds[group.key] || selectedVariant.id
                          }
                          onChange={(event) =>
                            handleVariantSelectionChange(group.key, event)
                          }
                        >
                          {group.variants.map((variant) => (
                            <option key={variant.id} value={variant.id}>
                              {variant.variantName || "Varianta unica"}
                            </option>
                          ))}
                        </select>
                      </div>

                      {selectedVariant.specificationValue != null &&
                        selectedVariant.specificationUnit && (
                          <p>
                            Specificatie:{" "}
                            <strong>
                              {selectedVariant.specificationValue}{" "}
                              {unitLabels[selectedVariant.specificationUnit]}
                            </strong>
                          </p>
                        )}

                      <p>
                        Cantitate curenta:{" "}
                        <strong>
                          {selectedVariant.currentQuantity}{" "}
                          {unitLabels[selectedVariant.baseUnit]}
                        </strong>
                      </p>

                      <p>
                        Prag minim: {selectedVariant.minimumQuantity}{" "}
                        {unitLabels[selectedVariant.baseUnit]}
                      </p>

                      <p>
                        Status:{" "}
                        <strong>
                          {selectedVariant.availableInWarehouse
                            ? "Disponibil"
                            : "Semnalat ca lipsa"}
                        </strong>
                      </p>

                      {belowMinimum && (
                        <p className="stock-warning-text">
                          Aceasta varianta este sub pragul minim.
                        </p>
                      )}

                      <div className="stock-card-actions">
                        <button
                          type="button"
                          onClick={() => handleSelectSupply(selectedVariant)}
                        >
                          Adauga intrare
                        </button>

                        {selectedVariant.availableInWarehouse ? (
                          <button
                            type="button"
                            onClick={() =>
                              handleMarkUnavailable(selectedVariant.id)
                            }
                          >
                            Semnaleaza lipsa
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() =>
                              handleMarkAvailable(selectedVariant.id)
                            }
                          >
                            Marcheaza disponibil
                          </button>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}

export default ManagerSuppliesPage;
