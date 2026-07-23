import ManagerSuppliesHeader from "../features/manager/supplies/components/ManagerSuppliesHeader";
import StockEntrySection from "../features/manager/supplies/components/StockEntrySection";
import StockZoneSection from "../features/manager/supplies/components/StockZoneSection";
import { STOCK_TYPES } from "../features/manager/supplies/constants/managerSuppliesConstants";
import useManagerSupplies from "../features/manager/supplies/hooks/useManagerSupplies";

function ManagerSuppliesPage() {
  const {
    supplies,
    groupedSupplies,
    selectedVariantIds,
    selectedSupply,
    entryForm,
    editingEntryId,
    entryHistory,
    loadingHistory,
    savingEntry,
    deletingEntryId,
    errorMessage,
    successMessage,
    compatibleInputUnits,
    handleVariantSelectionChange,
    handleSelectSupply,
    handleEntryFormChange,
    handleTargetSupplyChange,
    handlePackageTypeChange,
    handleSaveStockEntry,
    handleEditEntry,
    handleCancelEntryEdit,
    handleDeleteEntry,
    handleMarkUnavailable,
    handleMarkAvailable,
    handleBackToManager,
    handleCloseEntryForm,
  } = useManagerSupplies();

  return (
    <div className="manager-page">
      <ManagerSuppliesHeader onBackToManager={handleBackToManager} />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      {successMessage && <p className="feedback-message">{successMessage}</p>}

      {selectedSupply && (
        <StockEntrySection
          selectedSupply={selectedSupply}
          supplies={supplies}
          entryForm={entryForm}
          editingEntryId={editingEntryId}
          compatibleInputUnits={compatibleInputUnits}
          savingEntry={savingEntry}
          entryHistory={entryHistory}
          loadingHistory={loadingHistory}
          deletingEntryId={deletingEntryId}
          onSaveEntry={handleSaveStockEntry}
          onEntryFormChange={handleEntryFormChange}
          onTargetSupplyChange={handleTargetSupplyChange}
          onPackageTypeChange={handlePackageTypeChange}
          onCancelEntryEdit={handleCancelEntryEdit}
          onCloseEntryForm={handleCloseEntryForm}
          onEditEntry={handleEditEntry}
          onDeleteEntry={handleDeleteEntry}
        />
      )}

      {STOCK_TYPES.map((stockType) => (
        <StockZoneSection
          key={stockType}
          stockType={stockType}
          groupedSupplies={groupedSupplies}
          selectedVariantIds={selectedVariantIds}
          onVariantSelectionChange={handleVariantSelectionChange}
          onSelectSupply={handleSelectSupply}
          onMarkUnavailable={handleMarkUnavailable}
          onMarkAvailable={handleMarkAvailable}
        />
      ))}
    </div>
  );
}

export default ManagerSuppliesPage;
