import ClientMenuHeader from "../features/client/menu/components/ClientMenuHeader";
import MenuFeedbackSection from "../features/client/menu/components/MenuFeedbackSection";
import MenuFiltersSection from "../features/client/menu/components/MenuFiltersSection";
import MenuProductsSection from "../features/client/menu/components/MenuProductsSection";
import useClientMenu from "../features/client/menu/hooks/useClientMenu";

function ClientMenuPage() {
  const {
    categories,
    filteredProducts,
    selectedCategory,
    maxPrice,
    onlyAvailable,
    onlyVegetarian,
    onlyVegan,
    selectedMeatType,
    loadingProducts,
    productError,
    feedbackRating,
    feedbackComment,
    feedbackMessage,
    handleCategoryChange,
    handleMaxPriceChange,
    handleAvailabilityChange,
    handleVegetarianChange,
    handleVeganChange,
    handleMeatTypeChange,
    handleFeedbackRatingChange,
    handleFeedbackCommentChange,
    handleFeedbackSubmit,
  } = useClientMenu();

  if (loadingProducts) {
    return (
      <div className="client-menu-page">
        <ClientMenuHeader>
          <p>Se incarca produsele...</p>
        </ClientMenuHeader>
      </div>
    );
  }

  if (productError) {
    return (
      <div className="client-menu-page">
        <ClientMenuHeader>
          <p className="error-message">{productError}</p>
        </ClientMenuHeader>
      </div>
    );
  }

  return (
    <div className="client-menu-page">
      <ClientMenuHeader />

      <MenuFiltersSection
        categories={categories}
        selectedCategory={selectedCategory}
        maxPrice={maxPrice}
        onlyAvailable={onlyAvailable}
        onlyVegetarian={onlyVegetarian}
        onlyVegan={onlyVegan}
        selectedMeatType={selectedMeatType}
        onCategoryChange={handleCategoryChange}
        onMaxPriceChange={handleMaxPriceChange}
        onAvailabilityChange={handleAvailabilityChange}
        onVegetarianChange={handleVegetarianChange}
        onVeganChange={handleVeganChange}
        onMeatTypeChange={handleMeatTypeChange}
      />

      <MenuProductsSection products={filteredProducts} />

      <MenuFeedbackSection
        feedbackRating={feedbackRating}
        feedbackComment={feedbackComment}
        feedbackMessage={feedbackMessage}
        onRatingChange={handleFeedbackRatingChange}
        onCommentChange={handleFeedbackCommentChange}
        onSubmit={handleFeedbackSubmit}
      />
    </div>
  );
}

export default ClientMenuPage;
