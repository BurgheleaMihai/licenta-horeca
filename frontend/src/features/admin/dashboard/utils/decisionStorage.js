const DECISION_SUMMARY_STORAGE_KEY = "adminDecisionSummary";

const DECISION_UPDATED_AT_STORAGE_KEY = "adminDecisionUpdatedAt";

/*
 * Citeste ultima predictie pastrata pentru sesiunea
 * curenta a administratorului.
 */
export const readStoredDecisionSummary = () => {
  try {
    const storedValue = sessionStorage.getItem(DECISION_SUMMARY_STORAGE_KEY);

    return storedValue ? JSON.parse(storedValue) : null;
  } catch (error) {
    console.warn("Predictia salvata local nu a putut fi citita:", error);

    sessionStorage.removeItem(DECISION_SUMMARY_STORAGE_KEY);

    return null;
  }
};

/*
 * Citeste momentul ultimei actualizari si il
 * transforma intr-un obiect Date.
 */
export const readStoredDecisionUpdatedAt = () => {
  const storedValue = sessionStorage.getItem(DECISION_UPDATED_AT_STORAGE_KEY);

  if (!storedValue) {
    return null;
  }

  const storedDate = new Date(storedValue);

  if (Number.isNaN(storedDate.getTime())) {
    sessionStorage.removeItem(DECISION_UPDATED_AT_STORAGE_KEY);

    return null;
  }

  return storedDate;
};

/*
 * Salveaza predictia si momentul actualizarii
 * in sesiunea curenta a browserului.
 */
export const saveStoredDecisionData = (decisionSummary, updatedAt) => {
  try {
    sessionStorage.setItem(
      DECISION_SUMMARY_STORAGE_KEY,
      JSON.stringify(decisionSummary),
    );

    sessionStorage.setItem(
      DECISION_UPDATED_AT_STORAGE_KEY,
      updatedAt.toISOString(),
    );
  } catch (error) {
    console.warn("Predictia nu a putut fi salvata local:", error);
  }
};

/*
 * Elimina datele sistemului de decizie la logout.
 */
export const clearStoredDecisionData = () => {
  sessionStorage.removeItem(DECISION_SUMMARY_STORAGE_KEY);

  sessionStorage.removeItem(DECISION_UPDATED_AT_STORAGE_KEY);
};
