import {
  ALL_MEASUREMENT_UNITS,
  INITIAL_ENTRY_FORM,
} from "../constants/managerSuppliesConstants";

/*
 * Grupeaza variantele care apartin aceluiasi
 * produs si aceleiasi zone de stoc.
 */
export function groupSupplies(supplies) {
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
}

/*
 * Selecteaza initial prima varianta disponibila
 * pentru fiecare grup de produse.
 */
export function buildInitialVariantSelections(supplies) {
  const initialSelections = {};

  supplies.forEach((supply) => {
    const groupKey = `${supply.stockType}-${supply.name}`;

    if (!initialSelections[groupKey]) {
      initialSelections[groupKey] = String(supply.id);
    }
  });

  return initialSelections;
}

/*
 * Gaseste varianta selectata intr-un grup.
 */
export function getSelectedVariantForGroup(group, selectedVariantIds) {
  const selectedId = Number(selectedVariantIds[group.key]);

  return (
    group.variants.find((variant) => variant.id === selectedId) ||
    group.variants[0]
  );
}

/*
 * Construieste numele complet al produsului.
 */
export function getCompleteSupplyName(supply) {
  if (!supply) {
    return "";
  }

  if (!supply.variantName) {
    return supply.name;
  }

  return `${supply.name} - ${supply.variantName}`;
}

/*
 * Creeaza formularul initial pentru varianta
 * selectata.
 */
export function createEntryFormForSupply(supply) {
  return {
    ...INITIAL_ENTRY_FORM,

    supplyId: supply ? String(supply.id) : "",

    inputUnit: supply?.baseUnit || "PIECE",
  };
}

/*
 * Identifica varianta destinatie folosita
 * de formular.
 */
export function findTargetSupply(supplies, supplyId, selectedSupply) {
  if (!supplyId) {
    return selectedSupply;
  }

  return (
    supplies.find((supply) => supply.id === Number(supplyId)) || selectedSupply
  );
}

/*
 * Returneaza unitatile compatibile cu unitatea
 * de baza a produsului.
 */
export function getCompatibleInputUnits(targetSupply) {
  if (!targetSupply) {
    return ALL_MEASUREMENT_UNITS;
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
}

/*
 * Valideaza formularul si returneaza mesajul
 * de eroare. Un sir gol inseamna formular valid.
 */
export function getEntryFormValidationError(entryForm, editingEntryId) {
  const packageQuantity = Number(entryForm.packageQuantity);

  const quantityPerPackage = Number(entryForm.quantityPerPackage);

  if (Number.isNaN(packageQuantity) || packageQuantity <= 0) {
    return "Cantitatea primita trebuie sa fie " + "mai mare decat zero.";
  }

  if (
    entryForm.packageType !== "DIRECT" &&
    !Number.isInteger(packageQuantity)
  ) {
    return "Numarul de ambalaje trebuie sa fie " + "un numar intreg.";
  }

  if (
    entryForm.packageType !== "DIRECT" &&
    (Number.isNaN(quantityPerPackage) || quantityPerPackage <= 0)
  ) {
    return (
      "Cantitatea dintr-un ambalaj trebuie " + "sa fie mai mare decat zero."
    );
  }

  if (editingEntryId && !entryForm.supplyId) {
    return "Varianta destinatie este obligatorie.";
  }

  return "";
}

/*
 * Converteste formularul in obiectul trimis
 * catre backend.
 */
export function buildEntryData(entryForm, selectedSupply) {
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
}

/*
 * Verifica daca o varianta se afla sub
 * pragul minim configurat.
 */
export function isSupplyBelowMinimum(supply) {
  return Number(supply.currentQuantity) < Number(supply.minimumQuantity);
}

/*
 * Formateaza data unei intrari de stoc.
 */
export function formatStockEntryDate(dateValue) {
  if (!dateValue) {
    return "Data necunoscuta";
  }

  return new Date(dateValue).toLocaleString("ro-RO");
}
