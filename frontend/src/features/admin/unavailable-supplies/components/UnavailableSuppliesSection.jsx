const categoryLabels = {
  PACKAGING: "Ambalaje",
  CONSUMABLE: "Consumabile",
  BEVERAGE_INGREDIENT: "Ingrediente pentru bauturi",
  MEAT: "Carne",
  DAIRY: "Lactate",
  DRY_PRODUCT: "Produse uscate",
  FROZEN_PRODUCT: "Produse congelate",
  SAUCE: "Sosuri",
  OTHER_INGREDIENT: "Alte ingrediente",
  FRUIT: "Fructe",
  VEGETABLE: "Legume",
  OTHER: "Altele",
};

const stockTypeLabels = {
  AUXILIARY: "Auxiliar",
  WAREHOUSE: "Depozit",
  FRUIT_AND_VEGETABLE: "Fructe si legume",
};

const unitLabels = {
  PIECE: "buc",
  GRAM: "g",
  KILOGRAM: "kg",
  MILLILITER: "ml",
  LITER: "l",
};

function UnavailableSuppliesSection({ unavailableSupplies }) {
  /*
   * Componenta primeste de la AdminPage lista
   * articolelor semnalate ca indisponibile.
   *
   * Incarcarea datelor ramane in pagina parinte,
   * iar aceasta componenta se ocupa numai
   * de afisarea lor.
   */
  return (
    <section className="admin-section">
      <h2>Articole de stoc semnalate ca lipsa</h2>

      {unavailableSupplies.length === 0 ? (
        <p>Nu exista articole de stoc semnalate ca lipsa.</p>
      ) : (
        <div className="admin-grid">
          {unavailableSupplies.map((supply) => (
            <div key={supply.id} className="admin-card">
              <h3>{supply.name}</h3>

              <p>
                Zona:{" "}
                {stockTypeLabels[supply.stockType] ||
                  supply.stockType ||
                  "Necunoscuta"}
              </p>

              <p>
                Categorie: {categoryLabels[supply.category] || supply.category}
              </p>

              <p>
                Cantitate: {supply.currentQuantity ?? 0}{" "}
                {unitLabels[supply.baseUnit] || supply.baseUnit || ""}
              </p>

              <p>Status: lipsa in stoc</p>

              <p>
                Semnalat la:{" "}
                {supply.reportedAt
                  ? new Date(supply.reportedAt).toLocaleString()
                  : "necunoscut"}
              </p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default UnavailableSuppliesSection;
