import AdminDashboardHeader from "../features/admin/dashboard/components/AdminDashboardHeader";
import DecisionDashboardSection from "../features/admin/dashboard/components/DecisionDashboardSection";
import DecisionTrainingSection from "../features/admin/dashboard/components/DecisionTrainingSection";
import useAdminDecisionDashboard from "../features/admin/dashboard/hooks/useAdminDecisionDashboard";

function AdminDashboardPage() {
  const {
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
  } = useAdminDecisionDashboard();

  return (
    <div className="admin-page">
      <AdminDashboardHeader />

      <DecisionDashboardSection
        decisionSummary={decisionSummary}
        decisionUpdatedAt={decisionUpdatedAt}
        decisionLoading={decisionLoading}
        decisionError={decisionError}
        fallbackActive={fallbackActive}
        staffingRows={staffingRows}
        operationalRecommendations={operationalRecommendations}
        hasPendingPreviousPrediction={hasPendingPreviousPrediction}
        latestUnlabeledRecord={latestUnlabeledRecord}
        onRefresh={loadDecisionSummary}
      />

      <DecisionTrainingSection
        labelLoading={labelLoading}
        labelError={labelError}
        labelMessage={labelMessage}
        latestUnlabeledRecord={latestUnlabeledRecord}
        observedTrafficLevel={observedTrafficLevel}
        setObservedTrafficLevel={setObservedTrafficLevel}
        observedDelayRisk={observedDelayRisk}
        setObservedDelayRisk={setObservedDelayRisk}
        actualWaiters={actualWaiters}
        setActualWaiters={setActualWaiters}
        actualKitchenStaff={actualKitchenStaff}
        setActualKitchenStaff={setActualKitchenStaff}
        actualBarStaff={actualBarStaff}
        setActualBarStaff={setActualBarStaff}
        labelSaving={labelSaving}
        onLabelSubmit={handleLabelSubmit}
        retraining={retraining}
        retrainingMessage={retrainingMessage}
        retrainingError={retrainingError}
        onRetrainModels={handleRetrainModels}
      />
    </div>
  );
}

export default AdminDashboardPage;
