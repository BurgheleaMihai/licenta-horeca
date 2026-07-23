import WaiterActiveOrdersSection from "../features/waiter/dashboard/components/WaiterActiveOrdersSection";
import WaiterDashboardHeader from "../features/waiter/dashboard/components/WaiterDashboardHeader";
import WaiterNewOrderSection from "../features/waiter/dashboard/components/WaiterNewOrderSection";
import WaiterTablesSection from "../features/waiter/dashboard/components/WaiterTablesSection";
import useWaiterDashboard from "../features/waiter/dashboard/hooks/useWaiterDashboard";

function WaiterDashboardPage() {
  const {
    tables,
    orders,
    activeSessions,
    products,
    selectedTable,
    selectedSession,
    productQuantities,
    errorMessage,
    orderMessage,
    savingOrder,
    handleLogout,
    handleOpenQrPage,
    handleOpenSession,
    handleCreateOrderClick,
    handleCloseSession,
    handleQuantityChange,
    handleCancelOrder,
    handleSubmitOrder,
    handleMarkOrderAsServed,
    handleSendOrderToPreparation,
  } = useWaiterDashboard();

  return (
    <div className="waiter-page">
      <WaiterDashboardHeader
        onOpenQrPage={handleOpenQrPage}
        onLogout={handleLogout}
      />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      {orderMessage && <p className="success-message">{orderMessage}</p>}

      <WaiterTablesSection
        tables={tables}
        activeSessions={activeSessions}
        onOpenSession={handleOpenSession}
        onCreateOrder={handleCreateOrderClick}
        onCloseSession={handleCloseSession}
      />

      {selectedTable && selectedSession && (
        <WaiterNewOrderSection
          selectedTable={selectedTable}
          products={products}
          productQuantities={productQuantities}
          savingOrder={savingOrder}
          onQuantityChange={handleQuantityChange}
          onSubmitOrder={handleSubmitOrder}
          onCancelOrder={handleCancelOrder}
        />
      )}

      <WaiterActiveOrdersSection
        orders={orders}
        onSendToPreparation={handleSendOrderToPreparation}
        onMarkAsServed={handleMarkOrderAsServed}
      />
    </div>
  );
}

export default WaiterDashboardPage;
