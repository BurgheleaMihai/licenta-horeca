import { useEffect, useState } from "react";
import { getAllProducts, saveFeedback } from "../api/productApi";

function ClientMenuPage() {
  const [products, setProducts] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const [maxPrice, setMaxPrice] = useState("");
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [onlyVegetarian, setOnlyVegetarian] = useState(false);
  const [onlyVegan, setOnlyVegan] = useState(false);
  const [selectedMeatType, setSelectedMeatType] = useState("ALL");
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackComment, setFeedbackComment] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");

  useEffect(() => {
    getAllProducts()
      .then((response) => {
        setProducts(response.data);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea produselor:", error);
        setErrorMessage("Produsele nu au putut fi incarcate.");
        setLoading(false);
      });
  }, []);

  const handleFeedbackSubmit = (event) => {
    event.preventDefault();

    const feedback = {
      rating: Number(feedbackRating),
      comment: feedbackComment
    };

    saveFeedback(feedback)
      .then(() => {
        setFeedbackMessage("Feedback-ul a fost trimis cu succes.");
        setFeedbackRating(5);
        setFeedbackComment("");
      })
      .catch((error) => {
        console.error("Eroare la trimiterea feedback-ului:", error);
        setFeedbackMessage("Feedback-ul nu a putut fi trimis.");
      });
  };

  const categories = [
    "ALL",
    ...new Set(
      products
        .map((product) => product.category?.name)
        .filter(Boolean)
    )
  ];

  const filteredProducts = products.filter((product) => {
    const matchesCategory =
      selectedCategory === "ALL" || product.category?.name === selectedCategory;

    const matchesPrice =
      maxPrice === "" || Number(product.price) <= Number(maxPrice);

    const matchesAvailability = !onlyAvailable || product.available;

    const matchesVegetarian =
      !onlyVegetarian || product.vegetarian === true;

    const matchesVegan =
      !onlyVegan || product.vegan === true;

    const matchesMeatType =
      selectedMeatType === "ALL" || product.meatType === selectedMeatType;

    return (
      matchesCategory &&
      matchesPrice &&
      matchesAvailability &&
      matchesVegetarian &&
      matchesVegan &&
      matchesMeatType
    );
  });

  if (loading) {
    return (
      <div className="client-menu-page">
        <header className="menu-header">
          <h1>Meniu restaurant</h1>
          <p>Se incarca produsele...</p>
        </header>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className="client-menu-page">
        <header className="menu-header">
          <h1>Meniu restaurant</h1>
          <p className="error-message">{errorMessage}</p>
        </header>
      </div>
    );
  }

  return (
    <div className="client-menu-page">
      <header className="menu-header">
        <h1>Meniu restaurant</h1>
      </header>

      <section className="filters-section">
        <div className="filter-group">
          <label htmlFor="category-filter">Categorie</label>
          <select
            id="category-filter"
            value={selectedCategory}
            onChange={(event) => setSelectedCategory(event.target.value)}
          >
            {categories.map((category) => (
              <option key={category} value={category}>
                {category === "ALL" ? "Toate categoriile" : category}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="max-price-filter">Pret maxim</label>
          <input
            id="max-price-filter"
            type="number"
            placeholder="Ex: 35"
            value={maxPrice}
            onChange={(event) => setMaxPrice(event.target.value)}
          />
        </div>

        <label className="checkbox-filter">
          <input
            type="checkbox"
            checked={onlyAvailable}
            onChange={(event) => setOnlyAvailable(event.target.checked)}
          />
          <span>Afiseaza doar produse disponibile</span>
        </label>

        <label className="checkbox-filter">
          <input
            type="checkbox"
            checked={onlyVegetarian}
            onChange={(event) => setOnlyVegetarian(event.target.checked)}
          />
          <span>Vegetarian</span>
        </label>

        <label className="checkbox-filter">
          <input
            type="checkbox"
            checked={onlyVegan}
            onChange={(event) => setOnlyVegan(event.target.checked)}
          />
          <span>Vegan</span>
        </label>

        <div className="filter-group">
          <label htmlFor="meat-type-filter">Tip carne</label>
          <select
            id="meat-type-filter"
            value={selectedMeatType}
            onChange={(event) => setSelectedMeatType(event.target.value)}
          >
            <option value="ALL">Toate</option>
            <option value="none">Fara carne</option>
            <option value="porc">Porc</option>
            <option value="vita">Vita</option>
          </select>
        </div>
      </section>

      <p className="results-info">
        Produse afisate: {filteredProducts.length}
      </p>

      <section className="product-grid">
        {filteredProducts.map((product) => (
          <div
            key={product.id}
            className={`product-card ${product.available ? "" : "unavailable"}`}
          >
            <div className="product-card-header">
              <span className="product-category">
                {product.category?.name || "Fara categorie"}
              </span>

              <span className={product.available ? "available" : "not-available"}>
                {product.available ? "Disponibil" : "Indisponibil"}
              </span>
            </div>

            <h2>{product.name}</h2>
            <p>{product.description}</p>

            <div className="product-card-footer">
              <strong>{Number(product.price).toFixed(2)} lei</strong>
            </div>
          </div>
        ))}
      </section>

      <section className="feedback-section">
        <h2>Feedback anonim</h2>
        <p>
          După servire, clientul poate lăsa o opinie anonimă despre experiența avută.
        </p>

        <form onSubmit={handleFeedbackSubmit} className="feedback-form">
          <div className="filter-group">
            <label htmlFor="feedback-rating">Rating</label>
            <select
              id="feedback-rating"
              value={feedbackRating}
              onChange={(event) => setFeedbackRating(event.target.value)}
            >
              <option value="5">5 - Foarte bine</option>
              <option value="4">4 - Bine</option>
              <option value="3">3 - Mediu</option>
              <option value="2">2 - Slab</option>
              <option value="1">1 - Foarte slab</option>
            </select>
          </div>

          <div className="feedback-comment-group">
            <label htmlFor="feedback-comment">Comentariu</label>
            <textarea
              id="feedback-comment"
              value={feedbackComment}
              onChange={(event) => setFeedbackComment(event.target.value)}
              placeholder="Scrie un comentariu optional..."
              rows="4"
            />
          </div>

          <button type="submit" className="feedback-button">
            Trimite feedback
          </button>
        </form>

        {feedbackMessage && (
          <p className="feedback-message">{feedbackMessage}</p>
        )}
      </section>
    </div>
  );
}

export default ClientMenuPage;