import KitchenDashboardHeader from "../features/kitchen/dashboard/components/KitchenDashboardHeader";
import KitchenOrdersSection from "../features/kitchen/dashboard/components/KitchenOrdersSection";
import useKitchenDashboard from "../features/kitchen/dashboard/hooks/useKitchenDashboard";

function KitchenDashboardPage() {
  const {
    visibleOrders,
    errorMessage,
    handleLogout,
    handleMarkKitchenItemsAsReady,
  } = useKitchenDashboard();

  return (
    <div className="kitchen-page">
      <KitchenDashboardHeader onLogout={handleLogout} />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <KitchenOrdersSection
        orders={visibleOrders}
        onMarkItemsAsReady={handleMarkKitchenItemsAsReady}
      />
    </div>
  );
}

export default KitchenDashboardPage;
