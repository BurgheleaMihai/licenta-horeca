import ManagerActiveOrdersSection from "../features/manager/dashboard/components/ManagerActiveOrdersSection";
import ManagerDashboardHeader from "../features/manager/dashboard/components/ManagerDashboardHeader";
import ManagerFeedbackSection from "../features/manager/dashboard/components/ManagerFeedbackSection";
import ManagerOperationalStatusSection from "../features/manager/dashboard/components/ManagerOperationalStatusSection";
import ManagerStatisticsSection from "../features/manager/dashboard/components/ManagerStatisticsSection";
import ManagerStockRecommendationsSection from "../features/manager/dashboard/components/ManagerStockRecommendationsSection";
import ManagerTrafficSection from "../features/manager/dashboard/components/ManagerTrafficSection";
import useManagerDashboard from "../features/manager/dashboard/hooks/useManagerDashboard";

function ManagerDashboardPage() {
  const {
    activeOrders,
    feedbackList,
    statistics,
    selectedDate,
    startTime,
    endTime,
    appliedFilters,
    statisticsLoading,
    trafficSummary,
    errorMessage,
    newOrdersCount,
    inPreparationOrdersCount,
    readyOrdersCount,
    productSalesList,
    handleSelectedDateChange,
    handleStartTimeChange,
    handleEndTimeChange,
    handleStatisticsSubmit,
    handleResetStatistics,
  } = useManagerDashboard();

  return (
    <div className="manager-page">
      <ManagerDashboardHeader />

      {errorMessage && (
        <p className="error-message manager-page-message">{errorMessage}</p>
      )}

      <ManagerStatisticsSection
        selectedDate={selectedDate}
        startTime={startTime}
        endTime={endTime}
        appliedFilters={appliedFilters}
        statisticsLoading={statisticsLoading}
        statistics={statistics}
        onSelectedDateChange={handleSelectedDateChange}
        onStartTimeChange={handleStartTimeChange}
        onEndTimeChange={handleEndTimeChange}
        onSubmit={handleStatisticsSubmit}
        onReset={handleResetStatistics}
      />

      <ManagerTrafficSection trafficSummary={trafficSummary} />

      <ManagerOperationalStatusSection
        newOrdersCount={newOrdersCount}
        inPreparationOrdersCount={inPreparationOrdersCount}
        readyOrdersCount={readyOrdersCount}
      />

      <ManagerStockRecommendationsSection productSalesList={productSalesList} />

      <ManagerActiveOrdersSection activeOrders={activeOrders} />

      <ManagerFeedbackSection feedbackList={feedbackList} />
    </div>
  );
}

export default ManagerDashboardPage;
