import OrderHistoryFilterSection from "../features/admin/order-history/components/OrderHistoryFilterSection";
import OrderHistoryHeader from "../features/admin/order-history/components/OrderHistoryHeader";
import OrderHistoryResultsSection from "../features/admin/order-history/components/OrderHistoryResultsSection";
import useAdminOrderHistory from "../features/admin/order-history/hooks/useAdminOrderHistory";

function AdminOrderHistoryPage() {
  const {
    filters,
    resultsMode,
    expandedOrderId,
    loading,
    errorMessage,
    tableNumbers,
    displayedOrders,
    displayedOrdersValue,
    loadOrders,
    handleFilterChange,
    handleFilterSubmit,
    handleShowRecentOrders,
    handleResetFilters,
    handleToggleDetails,
  } = useAdminOrderHistory();

  return (
    <div className="admin-order-history-page">
      <OrderHistoryHeader />

      {errorMessage && (
        <p className="error-message admin-order-history-message">
          {errorMessage}
        </p>
      )}

      <OrderHistoryFilterSection
        filters={filters}
        tableNumbers={tableNumbers}
        loading={loading}
        onFilterChange={handleFilterChange}
        onSubmit={handleFilterSubmit}
        onShowRecentOrders={handleShowRecentOrders}
        onReset={handleResetFilters}
        onReload={loadOrders}
      />

      <OrderHistoryResultsSection
        loading={loading}
        resultsMode={resultsMode}
        displayedOrders={displayedOrders}
        displayedOrdersValue={displayedOrdersValue}
        expandedOrderId={expandedOrderId}
        onToggleDetails={handleToggleDetails}
      />
    </div>
  );
}

export default AdminOrderHistoryPage;
