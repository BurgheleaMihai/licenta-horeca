import { useState } from "react";

import {
  categoryLabels,
  stockTypeLabels,
  stockTypes,
  unitLabels,
} from "../constants/stockConfigurationConstants";

/*
 * Afiseaza produsele si variantele configurate.
 *
 * Componenta gestioneaza local filtrarea dupa
 * zona de stoc.
 */
function ConfiguredStockProductsSection({ groupedSupplies, onEdit, onDelete }) {
  const [selectedStockType, setSelectedStockType] = useState("ALL");

  const filteredGroups =
    selectedStockType === "ALL"
      ? groupedSupplies
      : groupedSupplies.filter(
          (group) => group.stockType === selectedStockType,
        );

  return (
    <section className="stock-configuration-section">
      <h2>Produse configurate</h2>

      <div className="stock-filter-buttons">
        <button
          type="button"
          className={
            selectedStockType === "ALL"
              ? "stock-filter-button active-filter"
              : "stock-filter-button"
          }
          onClick={() => setSelectedStockType("ALL")}
        >
          Toate
        </button>

        {stockTypes.map((stockType) => (
          <button
            key={stockType}
            type="button"
            className={
              selectedStockType === stockType
                ? "stock-filter-button active-filter"
                : "stock-filter-button"
            }
            onClick={() => setSelectedStockType(stockType)}
          >
            {stockTypeLabels[stockType]}
          </button>
        ))}
      </div>

      {filteredGroups.length === 0 ? (
        <p>Nu exista produse in aceasta zona.</p>
      ) : (
        <div className="stock-product-groups">
          {filteredGroups.map((group) => (
            <div
              key={`${group.stockType}-${group.name}`}
              className="stock-product-group"
            >
              <div className="stock-product-group-header">
                <h3>{group.name}</h3>

                <span>{stockTypeLabels[group.stockType]}</span>
              </div>

              <div className="stock-items-grid">
                {group.supplies.map((supply) => (
                  <article
                    key={supply.id}
                    className={
                      supply.active
                        ? "stock-item-card"
                        : "stock-item-card inactive-stock-item"
                    }
                  >
                    <h4>{supply.variantName || "Varianta unica"}</h4>

                    <p>
                      Categorie:{" "}
                      <strong>
                        {categoryLabels[supply.category] || supply.category}
                      </strong>
                    </p>

                    {supply.specificationValue != null &&
                      supply.specificationUnit && (
                        <p>
                          Specificatie:{" "}
                          <strong>
                            {supply.specificationValue}{" "}
                            {unitLabels[supply.specificationUnit] ||
                              supply.specificationUnit}
                          </strong>
                        </p>
                      )}

                    <p>
                      Cantitate:{" "}
                      <strong>
                        {supply.currentQuantity}{" "}
                        {unitLabels[supply.baseUnit] || supply.baseUnit}
                      </strong>
                    </p>

                    <p>
                      Prag minim:{" "}
                      <strong>
                        {supply.minimumQuantity}{" "}
                        {unitLabels[supply.baseUnit] || supply.baseUnit}
                      </strong>
                    </p>

                    <p>
                      Status:{" "}
                      <strong>{supply.active ? "Activa" : "Inactiva"}</strong>
                    </p>

                    <div className="stock-card-actions">
                      <button type="button" onClick={() => onEdit(supply)}>
                        Modifica
                      </button>

                      <button
                        type="button"
                        className="danger-button"
                        onClick={() => onDelete(supply)}
                      >
                        Sterge
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default ConfiguredStockProductsSection;
