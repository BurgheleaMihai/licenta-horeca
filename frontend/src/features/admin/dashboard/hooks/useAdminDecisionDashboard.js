import { useEffect, useState } from "react";

import {
  getDecisionSummary,
  getLatestUnlabeledDecisionRecord,
  labelDecisionRecord,
  retrainDecisionModels,
} from "../../../../api/decisionApi";
import {
  readStoredDecisionSummary,
  readStoredDecisionUpdatedAt,
  saveStoredDecisionData,
} from "../utils/decisionStorage";
import {
  buildOperationalRecommendations,
  getStaffingStatus,
  staffingRoles,
} from "../utils/decisionUtils";

/*
 * Gestioneaza starea si operatiile sistemului
 * de decizie din panoul administratorului.
 */
function useAdminDecisionDashboard() {
  const [decisionSummary, setDecisionSummary] = useState(
    readStoredDecisionSummary,
  );

  const [decisionLoading, setDecisionLoading] = useState(false);

  const [decisionError, setDecisionError] = useState("");

  const [decisionUpdatedAt, setDecisionUpdatedAt] = useState(
    readStoredDecisionUpdatedAt,
  );

  const [latestUnlabeledRecord, setLatestUnlabeledRecord] = useState(null);

  const [labelLoading, setLabelLoading] = useState(true);
  const [labelSaving, setLabelSaving] = useState(false);
  const [labelError, setLabelError] = useState("");
  const [labelMessage, setLabelMessage] = useState("");

  const [observedTrafficLevel, setObservedTrafficLevel] = useState("SCAZUT");

  const [observedDelayRisk, setObservedDelayRisk] = useState("SCAZUT");

  const [actualWaiters, setActualWaiters] = useState("0");

  const [actualKitchenStaff, setActualKitchenStaff] = useState("0");

  const [actualBarStaff, setActualBarStaff] = useState("0");

  const [retraining, setRetraining] = useState(false);

  const [retrainingMessage, setRetrainingMessage] = useState("");

  const [retrainingError, setRetrainingError] = useState("");

  /*
   * Incarca cea mai recenta predictie care nu
   * a fost inca etichetata cu rezultatele reale.
   */
  const loadLatestUnlabeledRecord = () => {
    setLabelLoading(true);
    setLabelError("");

    getLatestUnlabeledDecisionRecord()
      .then((response) => {
        const record = response.data;

        setLatestUnlabeledRecord(record);

        setObservedTrafficLevel(record.predictedTrafficLevel || "SCAZUT");

        setObservedDelayRisk(record.predictedDelayRisk || "SCAZUT");

        setActualWaiters(String(record.recommendedWaiters ?? 0));

        setActualKitchenStaff(String(record.recommendedKitchenStaff ?? 0));

        setActualBarStaff(String(record.recommendedBarStaff ?? 0));
      })
      .catch((error) => {
        if (error.response?.status === 404) {
          setLatestUnlabeledRecord(null);

          return;
        }

        console.error("Eroare la incarcarea predictiei neetichetate:", error);

        setLabelError("Ultima predictie neetichetata nu a putut fi incarcata.");
      })
      .finally(() => {
        setLabelLoading(false);
      });
  };

  /*
   * Solicita o analiza noua a situatiei
   * operationale curente.
   */
  const loadDecisionSummary = () => {
    setDecisionLoading(true);
    setDecisionError("");
    setLabelMessage("");
    setLabelError("");

    getDecisionSummary()
      .then((response) => {
        const updatedAt = new Date();

        setDecisionSummary(response.data);
        setDecisionUpdatedAt(updatedAt);

        saveStoredDecisionData(response.data, updatedAt);

        loadLatestUnlabeledRecord();
      })
      .catch((error) => {
        console.error("Eroare la incarcarea predictiilor:", error);

        setDecisionError(
          "Predictiile nu au putut fi incarcate. " +
            "Verifica daca AI Service este pornit.",
        );
      })
      .finally(() => {
        setDecisionLoading(false);
      });
  };

  useEffect(() => {
    getLatestUnlabeledDecisionRecord()
      .then((response) => {
        const record = response.data;

        setLatestUnlabeledRecord(record);

        setObservedTrafficLevel(record.predictedTrafficLevel || "SCAZUT");

        setObservedDelayRisk(record.predictedDelayRisk || "SCAZUT");

        setActualWaiters(String(record.recommendedWaiters ?? 0));

        setActualKitchenStaff(String(record.recommendedKitchenStaff ?? 0));

        setActualBarStaff(String(record.recommendedBarStaff ?? 0));
      })
      .catch((error) => {
        if (error.response?.status === 404) {
          setLatestUnlabeledRecord(null);

          return;
        }

        console.error("Eroare la incarcarea predictiei neetichetate:", error);

        setLabelError("Ultima predictie neetichetata nu a putut fi incarcata.");
      })
      .finally(() => {
        setLabelLoading(false);
      });
  }, []);

  /*
   * Salveaza rezultatele observate pentru
   * ultima predictie neetichetata.
   */
  const handleLabelSubmit = (event) => {
    event.preventDefault();

    if (!latestUnlabeledRecord?.id) {
      setLabelError("Nu exista nicio predictie care poate fi etichetata.");

      return;
    }

    if (
      actualWaiters === "" ||
      actualKitchenStaff === "" ||
      actualBarStaff === ""
    ) {
      setLabelError("Completeaza necesarul real observat pentru fiecare rol.");

      return;
    }

    const waiters = Number(actualWaiters);
    const kitchenStaff = Number(actualKitchenStaff);
    const barStaff = Number(actualBarStaff);

    if (
      Number.isNaN(waiters) ||
      Number.isNaN(kitchenStaff) ||
      Number.isNaN(barStaff) ||
      waiters < 0 ||
      kitchenStaff < 0 ||
      barStaff < 0
    ) {
      setLabelError("Necesarul observat trebuie sa fie zero sau mai mare.");

      return;
    }

    const labelData = {
      observedTrafficLevel,
      observedDelayRisk,
      actualWaiters: waiters,
      actualKitchenStaff: kitchenStaff,
      actualBarStaff: barStaff,
    };

    setLabelSaving(true);
    setLabelError("");
    setLabelMessage("");

    labelDecisionRecord(latestUnlabeledRecord.id, labelData)
      .then(() => {
        setLabelMessage(
          `Inregistrarea #${latestUnlabeledRecord.id} ` +
            "a fost etichetata cu succes.",
        );

        setLatestUnlabeledRecord(null);

        loadLatestUnlabeledRecord();
      })
      .catch((error) => {
        console.error("Eroare la salvarea label-urilor:", error);

        if (error.response?.status === 409) {
          setLabelError("Aceasta inregistrare a fost deja etichetata.");

          loadLatestUnlabeledRecord();

          return;
        }

        setLabelError("Rezultatele reale nu au putut fi salvate.");
      })
      .finally(() => {
        setLabelSaving(false);
      });
  };

  /*
   * Porneste reantrenarea modelelor dupa ce
   * exista suficiente inregistrari etichetate.
   */
  const handleRetrainModels = () => {
    setRetraining(true);
    setRetrainingMessage("");
    setRetrainingError("");

    retrainDecisionModels()
      .then((response) => {
        setRetrainingMessage(
          response.data?.message || "Reantrenarea a fost finalizata.",
        );
      })
      .catch((error) => {
        const message =
          error.response?.data?.message ||
          "Modelele nu au putut fi reantrenate.";

        if (error.response?.data?.status === "blocked") {
          setRetrainingMessage(message);

          return;
        }

        setRetrainingError(message);
      })
      .finally(() => {
        setRetraining(false);
      });
  };

  const staffingRows = decisionSummary
    ? staffingRoles.map((role) => {
        const deficit = decisionSummary[role.deficitField] ?? 0;

        return {
          ...role,
          active: decisionSummary[role.activeField] ?? 0,
          recommended: decisionSummary[role.recommendedField] ?? 0,
          deficit,
          status: getStaffingStatus(deficit),
        };
      })
    : [];

  const operationalRecommendations = decisionSummary
    ? buildOperationalRecommendations(decisionSummary)
    : [];

  const fallbackActive =
    decisionSummary?.trafficLevel === "NECUNOSCUT" ||
    decisionSummary?.delayRisk === "NECUNOSCUT";

  const hasPendingPreviousPrediction =
    !decisionSummary && !decisionLoading && latestUnlabeledRecord;

  return {
    decisionSummary,
    decisionLoading,
    decisionError,
    decisionUpdatedAt,
    latestUnlabeledRecord,
    labelLoading,
    labelSaving,
    labelError,
    labelMessage,
    observedTrafficLevel,
    setObservedTrafficLevel,
    observedDelayRisk,
    setObservedDelayRisk,
    actualWaiters,
    setActualWaiters,
    actualKitchenStaff,
    setActualKitchenStaff,
    actualBarStaff,
    setActualBarStaff,
    retraining,
    retrainingMessage,
    retrainingError,
    staffingRows,
    operationalRecommendations,
    fallbackActive,
    hasPendingPreviousPrediction,
    loadDecisionSummary,
    handleLabelSubmit,
    handleRetrainModels,
  };
}

export default useAdminDecisionDashboard;
