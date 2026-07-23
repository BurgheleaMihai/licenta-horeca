/*
 * Extrage denumirile unice ale produselor si le
 * sorteaza alfabetic folosind regulile limbii romane.
 */
export const getProductNames = (supplies) => {
  return [
    ...new Set(supplies.map((supply) => supply.name).filter(Boolean)),
  ].sort((firstName, secondName) =>
    firstName.localeCompare(secondName, "ro", {
      sensitivity: "base",
    }),
  );
};

/*
 * Grupeaza variantele dupa zona de stoc si
 * denumirea produsului de baza.
 *
 * Variantele din fiecare grup sunt sortate dupa
 * denumirea variantei, iar grupurile dupa produs.
 */
export const groupSuppliesByProduct = (supplies) => {
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
};

/*
 * Determina modul in care este definita varianta
 * unui articol existent.
 */
export const determineVariantType = (supply) => {
  if (supply.specificationValue != null && supply.specificationUnit) {
    return "MEASUREMENT";
  }

  if (supply.variantName) {
    return "TEXT";
  }

  return "NONE";
};

/*
 * Valideaza datele formularului de configurare.
 *
 * Returneaza un mesaj de eroare sau un sir gol
 * atunci cand formularul este valid.
 */
export const validateStockForm = (formData) => {
  if (!formData.name.trim()) {
    return "Denumirea produsului este obligatorie.";
  }

  const currentQuantity = Number(formData.currentQuantity);

  const minimumQuantity = Number(formData.minimumQuantity);

  if (Number.isNaN(currentQuantity) || Number.isNaN(minimumQuantity)) {
    return "Cantitatile introduse trebuie sa fie numere.";
  }

  if (currentQuantity < 0 || minimumQuantity < 0) {
    return "Cantitatile nu pot fi negative.";
  }

  if (formData.variantType === "TEXT" && !formData.variantName.trim()) {
    return "Denumirea variantei este obligatorie.";
  }

  if (formData.variantType === "MEASUREMENT") {
    const specificationValue = Number(formData.specificationValue);

    if (Number.isNaN(specificationValue) || specificationValue <= 0) {
      return "Valoarea variantei trebuie sa fie " + "mai mare decat zero.";
    }
  }

  return "";
};

/*
 * Transforma datele formularului in obiectul
 * trimis catre backend.
 */
export const buildSupplyData = (formData) => {
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
