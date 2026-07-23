/*
 * Configuratia rolurilor comparate in tabelul
 * de personal activ si personal recomandat.
 *
 * Fiecare rol indica numele campurilor primite
 * in raspunsul sistemului de decizie.
 */
export const staffingRoles = [
  {
    key: "waiters",
    label: "Ospatari",
    activeField: "activeWaiters",
    recommendedField: "recommendedWaiters",
    deficitField: "waiterDeficit",
  },
  {
    key: "kitchen",
    label: "Bucatarie",
    activeField: "activeKitchenStaff",
    recommendedField: "recommendedKitchenStaff",
    deficitField: "kitchenDeficit",
  },
  {
    key: "bar",
    label: "Bar",
    activeField: "activeBarStaff",
    recommendedField: "recommendedBarStaff",
    deficitField: "barDeficit",
  },
];

/*
 * Transforma nivelul returnat de backend intr-o
 * clasa CSS folosita pentru colorarea badge-urilor.
 */
export const getLevelClassName = (level) => {
  if (level === "RIDICAT") {
    return "high";
  }

  if (level === "MEDIU") {
    return "medium";
  }

  if (level === "SCAZUT") {
    return "low";
  }

  return "unknown";
};

/*
 * Construieste textul si clasa CSS pentru situatia
 * personalului dintr-un anumit rol.
 *
 * deficit > 0:
 * sunt necesari angajati suplimentari.
 *
 * deficit < 0:
 * exista mai multi angajati decat necesarul estimat.
 *
 * deficit === 0:
 * personalul activ este suficient.
 */
export const getStaffingStatus = (deficit) => {
  if (deficit > 0) {
    return {
      label: `Deficit ${deficit}`,
      className: "deficit",
    };
  }

  if (deficit < 0) {
    return {
      label: `Surplus ${Math.abs(deficit)}`,
      className: "surplus",
    };
  }

  return {
    label: "Suficient",
    className: "sufficient",
  };
};

/*
 * Genereaza recomandarile operationale afisate
 * administratorului.
 *
 * Recomandarile sunt determinate din:
 * - riscul de intarziere;
 * - nivelul de trafic;
 * - deficitul sau surplusul de personal;
 * - situatia fiecarui rol operational.
 */
export const buildOperationalRecommendations = (decisionSummary) => {
  const recommendations = [];

  if (decisionSummary.delayRisk === "RIDICAT") {
    recommendations.push({
      type: "urgent",
      text:
        "Riscul de intarziere este ridicat. " +
        "Prioritizeaza comenzile vechi si verifica " +
        "incarcarea din bucatarie si bar.",
    });
  }

  if (decisionSummary.trafficLevel === "RIDICAT") {
    recommendations.push({
      type: "warning",
      text:
        "Este estimat un varf de trafic. " +
        "Pregateste sectiile pentru un volum mai mare " +
        "de comenzi.",
    });
  }

  const roleRecommendations = [
    {
      label: "ospatari",
      deficit: decisionSummary.waiterDeficit,
      addMessage: (value) =>
        `Se recomanda suplimentarea turei cu ${value} ` +
        `${value === 1 ? "ospatar" : "ospatari"}.`,
      surplusMessage: (value) =>
        `Exista ${value} ${
          value === 1 ? "ospatar suplimentar" : "ospatari suplimentari"
        } care pot ajuta la alte activitati.`,
    },
    {
      label: "bucatarie",
      deficit: decisionSummary.kitchenDeficit,
      addMessage: (value) =>
        `Se recomanda suplimentarea bucatariei cu ${value} ` +
        `${value === 1 ? "angajat" : "angajati"}.`,
      surplusMessage: (value) =>
        `Bucataria are ${value} ${
          value === 1 ? "angajat suplimentar" : "angajati suplimentari"
        } fata de necesarul estimat.`,
    },
    {
      label: "bar",
      deficit: decisionSummary.barDeficit,
      addMessage: (value) =>
        `Se recomanda suplimentarea barului cu ${value} ` +
        `${value === 1 ? "angajat" : "angajati"}.`,
      surplusMessage: (value) =>
        `Barul are ${value} ${
          value === 1 ? "angajat suplimentar" : "angajati suplimentari"
        } care poate fi redistribuit.`,
    },
  ];

  roleRecommendations.forEach((role) => {
    if (role.deficit > 0) {
      recommendations.push({
        type: "warning",
        text: role.addMessage(role.deficit),
      });
    } else if (role.deficit < 0) {
      recommendations.push({
        type: "info",
        text: role.surplusMessage(Math.abs(role.deficit)),
      });
    }
  });

  const hasStaffDeficit = roleRecommendations.some((role) => role.deficit > 0);

  const hasStaffSurplus = roleRecommendations.some((role) => role.deficit < 0);

  if (!hasStaffDeficit && !hasStaffSurplus) {
    recommendations.push({
      type: "success",
      text:
        "Personalul activ corespunde necesarului " +
        "estimat pentru toate rolurile.",
    });
  }

  return recommendations;
};
