import StatisticsCalculationNotes from "../features/admin/statistics/components/StatisticsCalculationNotes";
import StatisticsFilterSection from "../features/admin/statistics/components/StatisticsFilterSection";
import StatisticsHeader from "../features/admin/statistics/components/StatisticsHeader";
import StatisticsIndicatorsSection from "../features/admin/statistics/components/StatisticsIndicatorsSection";
import TopSellingProductsSection from "../features/admin/statistics/components/TopSellingProductsSection";
import useAdminStatistics from "../features/admin/statistics/hooks/useAdminStatistics";

function AdminStatisticsPage() {
  const {
    period,
    customStartDate,
    customEndDate,
    startTime,
    endTime,
    appliedFilters,
    loading,
    errorMessage,
    statistics,
    handlePeriodChange,
    handleCustomStartDateChange,
    handleCustomEndDateChange,
    handleStartTimeChange,
    handleEndTimeChange,
    handleFilterSubmit,
    handleResetFilters,
  } = useAdminStatistics();

  return (
    <div className="admin-statistics-page">
      <StatisticsHeader />

      {errorMessage && (
        <p className="error-message admin-statistics-message">{errorMessage}</p>
      )}

      <StatisticsFilterSection
        period={period}
        customStartDate={customStartDate}
        customEndDate={customEndDate}
        startTime={startTime}
        endTime={endTime}
        appliedFilters={appliedFilters}
        onPeriodChange={handlePeriodChange}
        onCustomStartDateChange={handleCustomStartDateChange}
        onCustomEndDateChange={handleCustomEndDateChange}
        onStartTimeChange={handleStartTimeChange}
        onEndTimeChange={handleEndTimeChange}
        onSubmit={handleFilterSubmit}
        onReset={handleResetFilters}
      />

      <StatisticsIndicatorsSection loading={loading} statistics={statistics} />

      <TopSellingProductsSection
        loading={loading}
        productSalesList={statistics.productSalesList}
      />

      <StatisticsCalculationNotes />
    </div>
  );
}

export default AdminStatisticsPage;
