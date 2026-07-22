import { useEffect, useMemo, useState } from "react";
import {
  createAuxiliarySupply,
  deleteAuxiliarySupply,
  getAllAuxiliarySupplies,
  updateAuxiliarySupply,
} from "../api/auxiliarySupplyApi";

const initialFormData = {
  productSelection: "NEW",
  name: "",
  variantType: "NONE",
  variantName: "",
  specificationValue: "",
  specificationUnit: "MILLILITER",
  stockType: "AUXILIARY",
  category: "PACKAGING",
  baseUnit: "PIECE",
  currentQuantity: "0",
  minimumQuantity: "0",
  active: true,
};

const stockTypeLabels = {
  AUXILIARY: "Auxiliar",
  WAREHOUSE: "Depozit",
  FRUIT_AND_VEGETABLE: "Fructe si legume",
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

const variantTypeLabels = {
  NONE: "Fara varianta",
  MEASUREMENT: "Varianta numerica",
  TEXT: "Varianta text",
};

const stockTypes = ["AUXILIARY", "WAREHOUSE", "FRUIT_AND_VEGETABLE"];

const measurementUnits = ["PIECE", "GRAM", "KILOGRAM", "MILLILITER", "LITER"];

const categoriesByStockType = {
  AUXILIARY: ["PACKAGING", "CONSUMABLE", "BEVERAGE_INGREDIENT", "OTHER"],
  WAREHOUSE: [
    "MEAT",
    "DAIRY",
    "DRY_PRODUCT",
    "FROZEN_PRODUCT",
    "SAUCE",
    "OTHER_INGREDIENT",
    "OTHER",
  ],
  FRUIT_AND_VEGETABLE: ["FRUIT", "VEGETABLE", "OTHER"],
};

function AdminStockConfigurationPage() {
  const [supplies, setSupplies] = useState([]);
  const [formData, setFormData] = useState(initialFormData);
  const [editingSupplyId, setEditingSupplyId] = useState(null);
  const [selectedStockType, setSelectedStockType] = useState("ALL");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadSupplies();
  }, []);

  const productNames = useMemo(() => {
    return [
      ...new Set(supplies.map((supply) => supply.name).filter(Boolean)),
    ].sort((firstName, secondName) =>
      firstName.localeCompare(secondName, "ro", {
        sensitivity: "base",
      }),
    );
  }, [supplies]);

  const groupedSupplies = useMemo(() => {
    const groups = {};

    supplies.forEach((supply) => {
      const groupKey = `${supply.stockType}-${supply.name}`;

      if (!groups[groupKey]) {
        groups[groupKey] = {
          name: supply.name,
          stockType: supply.stockType,
          supplies: [],
        };
      }

      groups[groupKey].supplies.push(supply);
    });

    return Object.values(groups)
      .map((group) => ({
        ...group,
        supplies: [...group.supplies].sort((firstSupply, secondSupply) => {
          const firstVariant = firstSupply.variantName || "";

          const secondVariant = secondSupply.variantName || "";

          return firstVariant.localeCompare(secondVariant, "ro", {
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

  const loadSupplies = () => {
    setErrorMessage("");

    getAllAuxiliarySupplies()
      .then((response) => {
        setSupplies(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea articolelor de stoc:", error);

        setErrorMessage("Articolele de stoc nu au putut fi incarcate.");
      });
  };

  const resetForm = () => {
    setFormData(initialFormData);
    setEditingSupplyId(null);
    setErrorMessage("");
  };

  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  const handleInputChange = (event) => {
    const { name, value, type, checked } = event.target;

    setFormData((currentFormData) => ({
      ...currentFormData,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleProductSelectionChange = (event) => {
    const selectedValue = event.target.value;

    if (selectedValue === "NEW") {
      setFormData((currentFormData) => ({
        ...currentFormData,
        productSelection: "NEW",
        name: "",
      }));

      return;
    }

    const selectedProductSupply = supplies.find(
      (supply) => supply.name === selectedValue,
    );

    setFormData((currentFormData) => ({
      ...currentFormData,
      productSelection: selectedValue,
      name: selectedValue,
      stockType: selectedProductSupply?.stockType || currentFormData.stockType,
      category: selectedProductSupply?.category || currentFormData.category,
    }));
  };

  const handleStockTypeChange = (event) => {
    const newStockType = event.target.value;

    const firstAvailableCategory = categoriesByStockType[newStockType][0];

    setFormData((currentFormData) => ({
      ...currentFormData,
      stockType: newStockType,
      category: firstAvailableCategory,
    }));
  };

  const handleVariantTypeChange = (event) => {
    const newVariantType = event.target.value;

    setFormData((currentFormData) => ({
      ...currentFormData,
      variantType: newVariantType,
      variantName: "",
      specificationValue: "",
      specificationUnit: "MILLILITER",
    }));
  };

  const validateForm = () => {
    if (!formData.name.trim()) {
      setErrorMessage("Denumirea produsului este obligatorie.");

      return false;
    }

    const currentQuantity = Number(formData.currentQuantity);

    const minimumQuantity = Number(formData.minimumQuantity);

    if (Number.isNaN(currentQuantity) || Number.isNaN(minimumQuantity)) {
      setErrorMessage("Cantitatile introduse trebuie sa fie numere.");

      return false;
    }

    if (currentQuantity < 0 || minimumQuantity < 0) {
      setErrorMessage("Cantitatile nu pot fi negative.");

      return false;
    }

    if (formData.variantType === "TEXT" && !formData.variantName.trim()) {
      setErrorMessage("Denumirea variantei este obligatorie.");

      return false;
    }

    if (formData.variantType === "MEASUREMENT") {
      const specificationValue = Number(formData.specificationValue);

      if (Number.isNaN(specificationValue) || specificationValue <= 0) {
        setErrorMessage(
          "Valoarea variantei trebuie sa fie mai mare decat zero.",
        );

        return false;
      }
    }

    return true;
  };

  const buildSupplyData = () => {
    const supplyData = {
      name: formData.name.trim(),
      variantName: null,
      specificationValue: null,
      specificationUnit: null,
      stockType: formData.stockType,
      category: formData.category,
      baseUnit: formData.baseUnit,
      currentQuantity: Number(formData.currentQuantity),
      minimumQuantity: Number(formData.minimumQuantity),
      active: formData.active,
    };

    if (formData.variantType === "TEXT") {
      supplyData.variantName = formData.variantName.trim();
    }

    if (formData.variantType === "MEASUREMENT") {
      supplyData.specificationValue = Number(formData.specificationValue);

      supplyData.specificationUnit = formData.specificationUnit;
    }

    return supplyData;
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");
    setSuccessMessage("");

    if (!validateForm()) {
      return;
    }

    const supplyData = buildSupplyData();

    setSaving(true);

    const request = editingSupplyId
      ? updateAuxiliarySupply(editingSupplyId, supplyData)
      : createAuxiliarySupply(supplyData);

    request
      .then(() => {
        setSuccessMessage(
          editingSupplyId
            ? "Varianta de stoc a fost modificata."
            : "Varianta de stoc a fost adaugata.",
        );

        resetForm();
        loadSupplies();
      })
      .catch((error) => {
        console.error("Eroare la salvarea variantei:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Varianta de stoc nu a putut fi salvata.",
        );
      })
      .finally(() => {
        setSaving(false);
      });
  };

  const determineVariantType = (supply) => {
    if (supply.specificationValue != null && supply.specificationUnit) {
      return "MEASUREMENT";
    }

    if (supply.variantName) {
      return "TEXT";
    }

    return "NONE";
  };

  const handleEdit = (supply) => {
    const variantType = determineVariantType(supply);

    setEditingSupplyId(supply.id);
    setSuccessMessage("");
    setErrorMessage("");

    setFormData({
      productSelection: supply.name,
      name: supply.name || "",
      variantType,
      variantName: variantType === "TEXT" ? supply.variantName || "" : "",
      specificationValue:
        supply.specificationValue != null
          ? String(supply.specificationValue)
          : "",
      specificationUnit: supply.specificationUnit || "MILLILITER",
      stockType: supply.stockType || "AUXILIARY",
      category: supply.category || "OTHER",
      baseUnit: supply.baseUnit || "PIECE",
      currentQuantity: String(supply.currentQuantity ?? 0),
      minimumQuantity: String(supply.minimumQuantity ?? 0),
      active: supply.active ?? true,
    });

    globalThis.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleDelete = (supply) => {
    const completeName = supply.variantName
      ? `${supply.name} - ${supply.variantName}`
      : supply.name;

    const confirmed = globalThis.confirm(
      `Sigur doresti sa stergi "${completeName}"?`,
    );

    if (!confirmed) {
      return;
    }

    setErrorMessage("");
    setSuccessMessage("");

    deleteAuxiliarySupply(supply.id)
      .then(() => {
        setSuccessMessage("Varianta de stoc a fost stearsa.");

        if (editingSupplyId === supply.id) {
          resetForm();
        }

        loadSupplies();
      })
      .catch((error) => {
        console.error("Eroare la stergerea variantei:", error);

        setErrorMessage(
          "Varianta nu a putut fi stearsa. " +
            "Verifica daca are intrari de stoc salvate.",
        );
      });
  };

  const filteredGroups =
    selectedStockType === "ALL"
      ? groupedSupplies
      : groupedSupplies.filter(
          (group) => group.stockType === selectedStockType,
        );

  const availableCategories = categoriesByStockType[formData.stockType] || [
    "OTHER",
  ];

  const selectedExistingProduct = formData.productSelection !== "NEW";

  return (
    <div className="stock-configuration-page">
      <header className="stock-configuration-header">
        <h1>Configurare stocuri</h1>

        <p>Configureaza produsele de baza si variantele acestora.</p>

        <button type="button" onClick={handleBackToAdmin}>
          Inapoi la panoul de administrare
        </button>
      </header>

      {errorMessage && (
        <p className="error-message stock-page-message">{errorMessage}</p>
      )}

      {successMessage && (
        <p className="feedback-message stock-page-message">{successMessage}</p>
      )}

      <section className="stock-configuration-section">
        <h2>
          {editingSupplyId ? "Modifica varianta" : "Adauga produs sau varianta"}
        </h2>

        <form className="stock-configuration-form" onSubmit={handleSubmit}>
          <div className="stock-form-grid">
            <div className="filter-group">
              <label htmlFor="product-selection">Produs de baza</label>

              <select
                id="product-selection"
                name="productSelection"
                value={formData.productSelection}
                onChange={handleProductSelectionChange}
                disabled={Boolean(editingSupplyId)}
              >
                <option value="NEW">Produs nou</option>

                {productNames.map((productName) => (
                  <option key={productName} value={productName}>
                    {productName}
                  </option>
                ))}
              </select>
            </div>

            {!selectedExistingProduct && (
              <div className="filter-group">
                <label htmlFor="stock-name">Denumire produs nou</label>

                <input
                  id="stock-name"
                  name="name"
                  type="text"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="Exemplu: Pahare carton"
                />
              </div>
            )}

            {selectedExistingProduct && (
              <div className="filter-group">
                <label htmlFor="selected-name">Denumire produs</label>

                <input
                  id="selected-name"
                  type="text"
                  value={formData.name}
                  disabled
                />
              </div>
            )}

            <div className="filter-group">
              <label htmlFor="variant-type">Tip varianta</label>

              <select
                id="variant-type"
                name="variantType"
                value={formData.variantType}
                onChange={handleVariantTypeChange}
              >
                {Object.entries(variantTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            {formData.variantType === "TEXT" && (
              <div className="filter-group">
                <label htmlFor="variant-name">Denumire varianta</label>

                <input
                  id="variant-name"
                  name="variantName"
                  type="text"
                  value={formData.variantName}
                  onChange={handleInputChange}
                  placeholder="Exemplu: Large, Pui, Porc"
                />
              </div>
            )}

            {formData.variantType === "MEASUREMENT" && (
              <>
                <div className="filter-group">
                  <label htmlFor="specification-value">Valoare varianta</label>

                  <input
                    id="specification-value"
                    name="specificationValue"
                    type="number"
                    min="1"
                    step="1"
                    value={formData.specificationValue}
                    onChange={handleInputChange}
                    placeholder="Exemplu: 200"
                  />
                </div>

                <div className="filter-group">
                  <label htmlFor="specification-unit">Unitate varianta</label>

                  <select
                    id="specification-unit"
                    name="specificationUnit"
                    value={formData.specificationUnit}
                    onChange={handleInputChange}
                  >
                    {measurementUnits.map((unit) => (
                      <option key={unit} value={unit}>
                        {unitLabels[unit]}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            )}

            <div className="filter-group">
              <label htmlFor="stock-type">Zona de stoc</label>

              <select
                id="stock-type"
                name="stockType"
                value={formData.stockType}
                onChange={handleStockTypeChange}
                disabled={selectedExistingProduct}
              >
                {stockTypes.map((stockType) => (
                  <option key={stockType} value={stockType}>
                    {stockTypeLabels[stockType]}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="stock-category">Categorie</label>

              <select
                id="stock-category"
                name="category"
                value={formData.category}
                onChange={handleInputChange}
                disabled={selectedExistingProduct}
              >
                {availableCategories.map((category) => (
                  <option key={category} value={category}>
                    {categoryLabels[category]}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="base-unit">Unitate stoc</label>

              <select
                id="base-unit"
                name="baseUnit"
                value={formData.baseUnit}
                onChange={handleInputChange}
              >
                {measurementUnits.map((unit) => (
                  <option key={unit} value={unit}>
                    {unitLabels[unit]}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="current-quantity">Cantitate initiala</label>

              <input
                id="current-quantity"
                name="currentQuantity"
                type="number"
                min="0"
                step="1"
                value={formData.currentQuantity}
                onChange={handleInputChange}
              />
            </div>

            <div className="filter-group">
              <label htmlFor="minimum-quantity">Prag minim</label>

              <input
                id="minimum-quantity"
                name="minimumQuantity"
                type="number"
                min="0"
                step="1"
                value={formData.minimumQuantity}
                onChange={handleInputChange}
              />
            </div>
          </div>

          <label className="stock-active-checkbox">
            <input
              name="active"
              type="checkbox"
              checked={formData.active}
              onChange={handleInputChange}
            />
            Varianta activa
          </label>

          <div className="stock-form-actions">
            <button type="submit" disabled={saving}>
              {saving
                ? "Se salveaza..."
                : editingSupplyId
                  ? "Salveaza modificarile"
                  : "Adauga varianta"}
            </button>

            {editingSupplyId && (
              <button
                type="button"
                className="secondary-button"
                onClick={resetForm}
              >
                Renunta
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="stock-configuration-section">
        <h2>Produse configurate</h2>

        <div className="stock-filter-buttons">
          <button
            type="button"
            className={
              selectedStockType === "ALL"
                ? "stock-filter-button active-filter"
                : "stock-filter-button"
            }
            onClick={() => setSelectedStockType("ALL")}
          >
            Toate
          </button>

          {stockTypes.map((stockType) => (
            <button
              key={stockType}
              type="button"
              className={
                selectedStockType === stockType
                  ? "stock-filter-button active-filter"
                  : "stock-filter-button"
              }
              onClick={() => setSelectedStockType(stockType)}
            >
              {stockTypeLabels[stockType]}
            </button>
          ))}
        </div>

        {filteredGroups.length === 0 ? (
          <p>Nu exista produse in aceasta zona.</p>
        ) : (
          <div className="stock-product-groups">
            {filteredGroups.map((group) => (
              <div
                key={`${group.stockType}-${group.name}`}
                className="stock-product-group"
              >
                <div className="stock-product-group-header">
                  <h3>{group.name}</h3>

                  <span>{stockTypeLabels[group.stockType]}</span>
                </div>

                <div className="stock-items-grid">
                  {group.supplies.map((supply) => (
                    <article
                      key={supply.id}
                      className={
                        supply.active
                          ? "stock-item-card"
                          : "stock-item-card inactive-stock-item"
                      }
                    >
                      <h4>{supply.variantName || "Varianta unica"}</h4>

                      <p>
                        Categorie:{" "}
                        <strong>
                          {categoryLabels[supply.category] || supply.category}
                        </strong>
                      </p>

                      {supply.specificationValue != null &&
                        supply.specificationUnit && (
                          <p>
                            Specificatie:{" "}
                            <strong>
                              {supply.specificationValue}{" "}
                              {unitLabels[supply.specificationUnit]}
                            </strong>
                          </p>
                        )}

                      <p>
                        Cantitate:{" "}
                        <strong>
                          {supply.currentQuantity}{" "}
                          {unitLabels[supply.baseUnit] || supply.baseUnit}
                        </strong>
                      </p>

                      <p>
                        Prag minim:{" "}
                        <strong>
                          {supply.minimumQuantity}{" "}
                          {unitLabels[supply.baseUnit] || supply.baseUnit}
                        </strong>
                      </p>

                      <p>
                        Status:{" "}
                        <strong>{supply.active ? "Activa" : "Inactiva"}</strong>
                      </p>

                      <div className="stock-card-actions">
                        <button
                          type="button"
                          onClick={() => handleEdit(supply)}
                        >
                          Modifica
                        </button>

                        <button
                          type="button"
                          className="danger-button"
                          onClick={() => handleDelete(supply)}
                        >
                          Sterge
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default AdminStockConfigurationPage;
