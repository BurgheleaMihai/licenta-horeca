import { INITIAL_TRAFFIC_SUMMARY } from "../constants/trafficSimulatorConstants";

/*
 * Normalizeaza sumarul primit de la backend,
 * astfel incat toate valorile sa fie disponibile.
 */
export function normalizeTrafficSummary(summary) {
  return {
    entries: summary?.entries ?? INITIAL_TRAFFIC_SUMMARY.entries,

    exits: summary?.exits ?? INITIAL_TRAFFIC_SUMMARY.exits,

    estimatedOccupancy:
      summary?.estimatedOccupancy ?? INITIAL_TRAFFIC_SUMMARY.estimatedOccupancy,
  };
}
