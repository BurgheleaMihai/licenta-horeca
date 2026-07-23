import TrafficSensorSimulatorHeader from "../features/traffic/simulator/components/TrafficSensorSimulatorHeader";
import TrafficSummarySection from "../features/traffic/simulator/components/TrafficSummarySection";
import useTrafficSensorSimulator from "../features/traffic/simulator/hooks/useTrafficSensorSimulator";

function TrafficSensorSimulatorPage() {
  const { trafficSummary, handleSimulateEntry, handleSimulateExit } =
    useTrafficSensorSimulator();

  return (
    <div className="sensor-page">
      <TrafficSensorSimulatorHeader />

      <TrafficSummarySection
        trafficSummary={trafficSummary}
        onSimulateEntry={handleSimulateEntry}
        onSimulateExit={handleSimulateExit}
      />
    </div>
  );
}

export default TrafficSensorSimulatorPage;
