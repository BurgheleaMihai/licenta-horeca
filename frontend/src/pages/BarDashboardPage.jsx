import BarDashboardHeader from "../features/bar/dashboard/components/BarDashboardHeader";
import BarOrdersSection from "../features/bar/dashboard/components/BarOrdersSection";
import useBarDashboard from "../features/bar/dashboard/hooks/useBarDashboard";

function BarDashboardPage() {
  const {
    visibleOrders,
    errorMessage,
    handleLogout,
    handleMarkBarItemsAsReady,
  } = useBarDashboard();

  return (
    <div className="bar-page">
      <BarDashboardHeader onLogout={handleLogout} />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <BarOrdersSection
        orders={visibleOrders}
        onMarkItemsAsReady={handleMarkBarItemsAsReady}
      />
    </div>
  );
}

export default BarDashboardPage;
