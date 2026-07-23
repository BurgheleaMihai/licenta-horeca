import { useEffect, useMemo, useState } from "react";

import {
  createAuxiliarySupply,
  deleteAuxiliarySupply,
  getAllAuxiliarySupplies,
  updateAuxiliarySupply,
} from "../../../../api/auxiliarySupplyApi";
import {
  categoriesByStockType,
  initialFormData,
} from "../constants/stockConfigurationConstants";
import {
  buildSupplyData,
  determineVariantType,
  getProductNames,
  groupSuppliesByProduct,
  validateStockForm,
} from "../utils/stockConfigurationUtils";

/*
 * Gestioneaza datele, formularul si operatiile
 * paginii de configurare a stocurilor.
 */
function useStockConfiguration() {
  const [supplies, setSupplies] = useState([]);
  const [formData, setFormData] = useState(initialFormData);

  const [editingSupplyId, setEditingSupplyId] = useState(null);

  const [errorMessage, setErrorMessage] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  const [saving, setSaving] = useState(false);

  const productNames = useMemo(() => getProductNames(supplies), [supplies]);

  const groupedSupplies = useMemo(
    () => groupSuppliesByProduct(supplies),
    [supplies],
  );

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

  useEffect(() => {
    loadSupplies();
  }, []);

  const resetForm = () => {
    setFormData(initialFormData);
    setEditingSupplyId(null);
    setErrorMessage("");
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

  const handleSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");
    setSuccessMessage("");

    const validationError = validateStockForm(formData);

    if (validationError) {
      setErrorMessage(validationError);

      return;
    }

    const supplyData = buildSupplyData(formData);

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

  const availableCategories = categoriesByStockType[formData.stockType] || [
    "OTHER",
  ];

  const selectedExistingProduct = formData.productSelection !== "NEW";

  return {
    formData,
    editingSupplyId,
    errorMessage,
    successMessage,
    saving,
    productNames,
    groupedSupplies,
    availableCategories,
    selectedExistingProduct,
    resetForm,
    handleInputChange,
    handleProductSelectionChange,
    handleStockTypeChange,
    handleVariantTypeChange,
    handleSubmit,
    handleEdit,
    handleDelete,
  };
}

export default useStockConfiguration;
