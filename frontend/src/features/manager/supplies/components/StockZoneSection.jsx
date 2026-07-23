import {
  CATEGORY_LABELS,
  STOCK_TYPE_LABELS,
  UNIT_LABELS,
} from "../constants/managerSuppliesConstants";
import {
  getSelectedVariantForGroup,
  isSupplyBelowMinimum,
} from "../utils/managerSuppliesUtils";

function StockZoneSection({
  stockType,
  groupedSupplies,
  selectedVariantIds,
  onVariantSelectionChange,
  onSelectSupply,
  onMarkUnavailable,
  onMarkAvailable,
}) {
  const groupsFromZone = groupedSupplies.filter(
    (group) => group.stockType === stockType,
  );

  return (
    <section className="manager-section">
      <h2>{STOCK_TYPE_LABELS[stockType]}</h2>

      {groupsFromZone.length === 0 ? (
        <p>Nu exista produse configurate in aceasta zona.</p>
      ) : (
        <div className="manager-grid">
          {groupsFromZone.map((group) => {
            const selectedVariant = getSelectedVariantForGroup(
              group,
              selectedVariantIds,
            );

            const belowMinimum = isSupplyBelowMinimum(selectedVariant);

            const warning =
              belowMinimum || !selectedVariant.availableInWarehouse;

            return (
              <article
                key={group.key}
                className={
                  warning ? "manager-card stock-warning-card" : "manager-card"
                }
              >
                <h3>{group.name}</h3>

                <p>
                  Categorie: {CATEGORY_LABELS[group.category] || group.category}
                </p>

                <div className="filter-group">
                  <label htmlFor={`variant-${group.key}`}>Varianta</label>

                  <select
                    id={`variant-${group.key}`}
                    value={selectedVariantIds[group.key] || selectedVariant.id}
                    onChange={(event) =>
                      onVariantSelectionChange(group.key, event)
                    }
                  >
                    {group.variants.map((variant) => (
                      <option key={variant.id} value={variant.id}>
                        {variant.variantName || "Varianta unica"}
                      </option>
                    ))}
                  </select>
                </div>

                {selectedVariant.specificationValue != null &&
                  selectedVariant.specificationUnit && (
                    <p>
                      Specificatie:{" "}
                      <strong>
                        {selectedVariant.specificationValue}{" "}
                        {UNIT_LABELS[selectedVariant.specificationUnit]}
                      </strong>
                    </p>
                  )}

                <p>
                  Cantitate curenta:{" "}
                  <strong>
                    {selectedVariant.currentQuantity}{" "}
                    {UNIT_LABELS[selectedVariant.baseUnit]}
                  </strong>
                </p>

                <p>
                  Prag minim: {selectedVariant.minimumQuantity}{" "}
                  {UNIT_LABELS[selectedVariant.baseUnit]}
                </p>

                <p>
                  Status:{" "}
                  <strong>
                    {selectedVariant.availableInWarehouse
                      ? "Disponibil"
                      : "Semnalat ca lipsa"}
                  </strong>
                </p>

                {belowMinimum && (
                  <p className="stock-warning-text">
                    Aceasta varianta este sub pragul minim.
                  </p>
                )}

                <div className="stock-card-actions">
                  <button
                    type="button"
                    onClick={() => onSelectSupply(selectedVariant)}
                  >
                    Adauga intrare
                  </button>

                  {selectedVariant.availableInWarehouse ? (
                    <button
                      type="button"
                      onClick={() => onMarkUnavailable(selectedVariant.id)}
                    >
                      Semnaleaza lipsa
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => onMarkAvailable(selectedVariant.id)}
                    >
                      Marcheaza disponibil
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default StockZoneSection;
