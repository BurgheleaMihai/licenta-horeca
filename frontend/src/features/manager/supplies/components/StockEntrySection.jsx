import {
  PACKAGE_TYPES,
  PACKAGE_TYPE_LABELS,
  UNIT_LABELS,
} from "../constants/managerSuppliesConstants";
import {
  formatStockEntryDate,
  getCompleteSupplyName,
} from "../utils/managerSuppliesUtils";

function StockEntrySection({
  selectedSupply,
  supplies,
  entryForm,
  editingEntryId,
  compatibleInputUnits,
  savingEntry,
  entryHistory,
  loadingHistory,
  deletingEntryId,
  onSaveEntry,
  onEntryFormChange,
  onTargetSupplyChange,
  onPackageTypeChange,
  onCancelEntryEdit,
  onCloseEntryForm,
  onEditEntry,
  onDeleteEntry,
}) {
  return (
    <section className="manager-section">
      <h2>
        {editingEntryId ? "Modifica intrarea" : "Intrare marfa"} -{" "}
        {getCompleteSupplyName(selectedSupply)}
      </h2>

      <div className="stock-entry-summary">
        <p>
          Produs: <strong>{selectedSupply.name}</strong>
        </p>

        <p>
          Varianta deschisa:{" "}
          <strong>{selectedSupply.variantName || "Varianta unica"}</strong>
        </p>

        <p>
          Stoc curent:{" "}
          <strong>
            {selectedSupply.currentQuantity}{" "}
            {UNIT_LABELS[selectedSupply.baseUnit]}
          </strong>
        </p>
      </div>

      <form className="stock-entry-form" onSubmit={onSaveEntry}>
        <div className="stock-entry-form-grid">
          {editingEntryId && (
            <div className="filter-group">
              <label htmlFor="entry-supply">Produs si varianta</label>

              <select
                id="entry-supply"
                name="supplyId"
                value={entryForm.supplyId}
                onChange={onTargetSupplyChange}
              >
                {supplies.map((supply) => (
                  <option key={supply.id} value={supply.id}>
                    {getCompleteSupplyName(supply)}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="filter-group">
            <label htmlFor="package-type">Tipul intrarii</label>

            <select
              id="package-type"
              name="packageType"
              value={entryForm.packageType}
              onChange={onPackageTypeChange}
            >
              {PACKAGE_TYPES.map((packageType) => (
                <option key={packageType} value={packageType}>
                  {PACKAGE_TYPE_LABELS[packageType]}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="package-quantity">
              {entryForm.packageType === "DIRECT"
                ? "Cantitate primita"
                : "Numar de ambalaje"}
            </label>

            <input
              id="package-quantity"
              name="packageQuantity"
              type="number"
              min={entryForm.packageType === "DIRECT" ? "0.001" : "1"}
              step={entryForm.packageType === "DIRECT" ? "0.001" : "1"}
              value={entryForm.packageQuantity}
              onChange={onEntryFormChange}
            />
          </div>

          {entryForm.packageType !== "DIRECT" && (
            <div className="filter-group">
              <label htmlFor="quantity-per-package">
                Cantitate intr-un ambalaj
              </label>

              <input
                id="quantity-per-package"
                name="quantityPerPackage"
                type="number"
                min="0.001"
                step="0.001"
                value={entryForm.quantityPerPackage}
                onChange={onEntryFormChange}
              />
            </div>
          )}

          <div className="filter-group">
            <label htmlFor="input-unit">Unitatea de pe ambalaj</label>

            <select
              id="input-unit"
              name="inputUnit"
              value={entryForm.inputUnit}
              onChange={onEntryFormChange}
            >
              {compatibleInputUnits.map((unit) => (
                <option key={unit} value={unit}>
                  {UNIT_LABELS[unit]}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="entry-notes">Observatii</label>

            <input
              id="entry-notes"
              name="notes"
              type="text"
              value={entryForm.notes}
              onChange={onEntryFormChange}
              placeholder="Optional"
            />
          </div>
        </div>

        <div className="stock-entry-actions">
          <button type="submit" disabled={savingEntry}>
            {savingEntry
              ? "Se salveaza..."
              : editingEntryId
                ? "Salveaza modificarea"
                : "Adauga intrarea"}
          </button>

          {editingEntryId && (
            <button
              type="button"
              className="secondary-button"
              onClick={onCancelEntryEdit}
            >
              Renunta la modificare
            </button>
          )}

          <button
            type="button"
            className="secondary-button"
            onClick={onCloseEntryForm}
          >
            Inchide
          </button>
        </div>
      </form>

      <div className="stock-entry-history">
        <h3>Istoric intrari</h3>

        {loadingHistory ? (
          <p>Se incarca istoricul...</p>
        ) : entryHistory.length === 0 ? (
          <p>Nu exista intrari salvate pentru aceasta varianta.</p>
        ) : (
          <div className="manager-grid">
            {entryHistory.map((entry) => (
              <article key={entry.id} className="manager-card">
                <h4>{PACKAGE_TYPE_LABELS[entry.packageType]}</h4>

                <p>
                  Varianta:{" "}
                  <strong>{getCompleteSupplyName(entry.supply)}</strong>
                </p>

                <p>Ambalaje/cantitate: {entry.packageQuantity}</p>

                {entry.packageType !== "DIRECT" && (
                  <p>
                    Cantitate per ambalaj: {entry.quantityPerPackage}{" "}
                    {UNIT_LABELS[entry.inputUnit]}
                  </p>
                )}

                <p>
                  Cantitate adaugata:{" "}
                  <strong>
                    {entry.convertedQuantity}{" "}
                    {UNIT_LABELS[entry.supply?.baseUnit]}
                  </strong>
                </p>

                <p>Stoc anterior: {entry.previousQuantity}</p>

                <p>Stoc nou: {entry.newQuantity}</p>

                <p>Data: {formatStockEntryDate(entry.createdAt)}</p>

                {entry.notes && <p>Observatii: {entry.notes}</p>}

                <div className="stock-card-actions">
                  <button type="button" onClick={() => onEditEntry(entry)}>
                    Modifica
                  </button>

                  <button
                    type="button"
                    className="danger-button"
                    disabled={deletingEntryId === entry.id}
                    onClick={() => onDeleteEntry(entry)}
                  >
                    {deletingEntryId === entry.id ? "Se sterge..." : "Sterge"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

export default StockEntrySection;
