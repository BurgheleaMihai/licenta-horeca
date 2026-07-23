import {
  categoryLabels,
  measurementUnits,
  stockTypeLabels,
  stockTypes,
  unitLabels,
  variantTypeLabels,
} from "../constants/stockConfigurationConstants";

/*
 * Formularul folosit pentru adaugarea si
 * modificarea variantelor de stoc.
 *
 * Datele si actiunile sunt primite de la
 * AdminStockConfigurationPage.
 */
function StockConfigurationForm({
  formData,
  editingSupplyId,
  productNames,
  selectedExistingProduct,
  availableCategories,
  saving,
  onSubmit,
  onInputChange,
  onProductSelectionChange,
  onStockTypeChange,
  onVariantTypeChange,
  onReset,
}) {
  return (
    <section className="stock-configuration-section">
      <h2>
        {editingSupplyId ? "Modifica varianta" : "Adauga produs sau varianta"}
      </h2>

      <form className="stock-configuration-form" onSubmit={onSubmit}>
        <div className="stock-form-grid">
          <div className="filter-group">
            <label htmlFor="product-selection">Produs de baza</label>

            <select
              id="product-selection"
              name="productSelection"
              value={formData.productSelection}
              onChange={onProductSelectionChange}
              disabled={Boolean(editingSupplyId)}
            >
              <option value="NEW">Produs nou</option>

              {productNames.map((productName) => (
                <option key={productName} value={productName}>
                  {productName}
                </option>
              ))}
            </select>
          </div>

          {!selectedExistingProduct && (
            <div className="filter-group">
              <label htmlFor="stock-name">Denumire produs nou</label>

              <input
                id="stock-name"
                name="name"
                type="text"
                value={formData.name}
                onChange={onInputChange}
                placeholder="Exemplu: Pahare carton"
              />
            </div>
          )}

          {selectedExistingProduct && (
            <div className="filter-group">
              <label htmlFor="selected-name">Denumire produs</label>

              <input
                id="selected-name"
                type="text"
                value={formData.name}
                disabled
              />
            </div>
          )}

          <div className="filter-group">
            <label htmlFor="variant-type">Tip varianta</label>

            <select
              id="variant-type"
              name="variantType"
              value={formData.variantType}
              onChange={onVariantTypeChange}
            >
              {Object.entries(variantTypeLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          {formData.variantType === "TEXT" && (
            <div className="filter-group">
              <label htmlFor="variant-name">Denumire varianta</label>

              <input
                id="variant-name"
                name="variantName"
                type="text"
                value={formData.variantName}
                onChange={onInputChange}
                placeholder="Exemplu: Large, Pui, Porc"
              />
            </div>
          )}

          {formData.variantType === "MEASUREMENT" && (
            <>
              <div className="filter-group">
                <label htmlFor="specification-value">Valoare varianta</label>

                <input
                  id="specification-value"
                  name="specificationValue"
                  type="number"
                  min="1"
                  step="1"
                  value={formData.specificationValue}
                  onChange={onInputChange}
                  placeholder="Exemplu: 200"
                />
              </div>

              <div className="filter-group">
                <label htmlFor="specification-unit">Unitate varianta</label>

                <select
                  id="specification-unit"
                  name="specificationUnit"
                  value={formData.specificationUnit}
                  onChange={onInputChange}
                >
                  {measurementUnits.map((unit) => (
                    <option key={unit} value={unit}>
                      {unitLabels[unit]}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}

          <div className="filter-group">
            <label htmlFor="stock-type">Zona de stoc</label>

            <select
              id="stock-type"
              name="stockType"
              value={formData.stockType}
              onChange={onStockTypeChange}
              disabled={selectedExistingProduct}
            >
              {stockTypes.map((stockType) => (
                <option key={stockType} value={stockType}>
                  {stockTypeLabels[stockType]}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="stock-category">Categorie</label>

            <select
              id="stock-category"
              name="category"
              value={formData.category}
              onChange={onInputChange}
              disabled={selectedExistingProduct}
            >
              {availableCategories.map((category) => (
                <option key={category} value={category}>
                  {categoryLabels[category]}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="base-unit">Unitate stoc</label>

            <select
              id="base-unit"
              name="baseUnit"
              value={formData.baseUnit}
              onChange={onInputChange}
            >
              {measurementUnits.map((unit) => (
                <option key={unit} value={unit}>
                  {unitLabels[unit]}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="current-quantity">Cantitate initiala</label>

            <input
              id="current-quantity"
              name="currentQuantity"
              type="number"
              min="0"
              step="1"
              value={formData.currentQuantity}
              onChange={onInputChange}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="minimum-quantity">Prag minim</label>

            <input
              id="minimum-quantity"
              name="minimumQuantity"
              type="number"
              min="0"
              step="1"
              value={formData.minimumQuantity}
              onChange={onInputChange}
            />
          </div>
        </div>

        <label className="stock-active-checkbox">
          <input
            name="active"
            type="checkbox"
            checked={formData.active}
            onChange={onInputChange}
          />
          Varianta activa
        </label>

        <div className="stock-form-actions">
          <button type="submit" disabled={saving}>
            {saving
              ? "Se salveaza..."
              : editingSupplyId
                ? "Salveaza modificarile"
                : "Adauga varianta"}
          </button>

          {editingSupplyId && (
            <button
              type="button"
              className="secondary-button"
              onClick={onReset}
            >
              Renunta
            </button>
          )}
        </div>
      </form>
    </section>
  );
}

export default StockConfigurationForm;
