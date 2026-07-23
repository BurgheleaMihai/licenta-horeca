import ConfiguredStockProductsSection from "../features/admin/stock-configuration/components/ConfiguredStockProductsSection";
import StockConfigurationForm from "../features/admin/stock-configuration/components/StockConfigurationForm";
import StockConfigurationHeader from "../features/admin/stock-configuration/components/StockConfigurationHeader";
import useStockConfiguration from "../features/admin/stock-configuration/hooks/useStockConfiguration";

function AdminStockConfigurationPage() {
  const {
    formData,
    editingSupplyId,
    errorMessage,
    successMessage,
    saving,
    productNames,
    groupedSupplies,
    availableCategories,
    selectedExistingProduct,
    resetForm,
    handleInputChange,
    handleProductSelectionChange,
    handleStockTypeChange,
    handleVariantTypeChange,
    handleSubmit,
    handleEdit,
    handleDelete,
  } = useStockConfiguration();

  return (
    <div className="stock-configuration-page">
      <StockConfigurationHeader />

      {errorMessage && (
        <p className="error-message stock-page-message">{errorMessage}</p>
      )}

      {successMessage && (
        <p className="feedback-message stock-page-message">{successMessage}</p>
      )}

      <StockConfigurationForm
        formData={formData}
        editingSupplyId={editingSupplyId}
        productNames={productNames}
        selectedExistingProduct={selectedExistingProduct}
        availableCategories={availableCategories}
        saving={saving}
        onSubmit={handleSubmit}
        onInputChange={handleInputChange}
        onProductSelectionChange={handleProductSelectionChange}
        onStockTypeChange={handleStockTypeChange}
        onVariantTypeChange={handleVariantTypeChange}
        onReset={resetForm}
      />

      <ConfiguredStockProductsSection
        groupedSupplies={groupedSupplies}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />
    </div>
  );
}

export default AdminStockConfigurationPage;
