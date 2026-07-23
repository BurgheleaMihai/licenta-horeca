import {
  ALL_FILTER_VALUE,
  MEAT_TYPE_OPTIONS,
} from "../constants/clientMenuConstants";

function MenuFiltersSection({
  categories,
  selectedCategory,
  maxPrice,
  onlyAvailable,
  onlyVegetarian,
  onlyVegan,
  selectedMeatType,
  onCategoryChange,
  onMaxPriceChange,
  onAvailabilityChange,
  onVegetarianChange,
  onVeganChange,
  onMeatTypeChange,
}) {
  return (
    <section className="filters-section">
      <div className="filter-group">
        <label htmlFor="category-filter">Categorie</label>

        <select
          id="category-filter"
          value={selectedCategory}
          onChange={onCategoryChange}
        >
          {categories.map((category) => (
            <option key={category} value={category}>
              {category === ALL_FILTER_VALUE ? "Toate categoriile" : category}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-group">
        <label htmlFor="max-price-filter">Pret maxim</label>

        <input
          id="max-price-filter"
          type="number"
          min="0"
          placeholder="Ex: 35"
          value={maxPrice}
          onChange={onMaxPriceChange}
        />
      </div>

      <label className="checkbox-filter">
        <input
          type="checkbox"
          checked={onlyAvailable}
          onChange={onAvailabilityChange}
        />

        <span>Afiseaza doar produse disponibile</span>
      </label>

      <label className="checkbox-filter">
        <input
          type="checkbox"
          checked={onlyVegetarian}
          onChange={onVegetarianChange}
        />

        <span>Vegetarian</span>
      </label>

      <label className="checkbox-filter">
        <input type="checkbox" checked={onlyVegan} onChange={onVeganChange} />

        <span>Vegan</span>
      </label>

      <div className="filter-group">
        <label htmlFor="meat-type-filter">Tip carne</label>

        <select
          id="meat-type-filter"
          value={selectedMeatType}
          onChange={onMeatTypeChange}
        >
          {MEAT_TYPE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
    </section>
  );
}

export default MenuFiltersSection;
