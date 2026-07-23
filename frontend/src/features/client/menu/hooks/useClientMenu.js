import { useEffect, useMemo, useState } from "react";

import { getAllProducts, saveFeedback } from "../../../../api/productApi";
import {
  ALL_FILTER_VALUE,
  INITIAL_FEEDBACK_RATING,
} from "../constants/clientMenuConstants";
import {
  filterMenuProducts,
  getProductCategories,
} from "../utils/clientMenuUtils";

function useClientMenu() {
  const [products, setProducts] = useState([]);

  const [selectedCategory, setSelectedCategory] = useState(ALL_FILTER_VALUE);

  const [maxPrice, setMaxPrice] = useState("");

  const [onlyAvailable, setOnlyAvailable] = useState(false);

  const [onlyVegetarian, setOnlyVegetarian] = useState(false);

  const [onlyVegan, setOnlyVegan] = useState(false);

  const [selectedMeatType, setSelectedMeatType] = useState(ALL_FILTER_VALUE);

  const [loadingProducts, setLoadingProducts] = useState(true);

  const [productError, setProductError] = useState("");

  const [feedbackRating, setFeedbackRating] = useState(INITIAL_FEEDBACK_RATING);

  const [feedbackComment, setFeedbackComment] = useState("");

  const [feedbackMessage, setFeedbackMessage] = useState("");

  useEffect(() => {
    getAllProducts()
      .then((response) => {
        setProducts(Array.isArray(response.data) ? response.data : []);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea produselor:", error);

        setProductError("Produsele nu au putut fi incarcate.");
      })
      .finally(() => {
        setLoadingProducts(false);
      });
  }, []);

  const categories = useMemo(() => getProductCategories(products), [products]);

  const filteredProducts = useMemo(
    () =>
      filterMenuProducts(products, {
        selectedCategory,
        maxPrice,
        onlyAvailable,
        onlyVegetarian,
        onlyVegan,
        selectedMeatType,
      }),
    [
      products,
      selectedCategory,
      maxPrice,
      onlyAvailable,
      onlyVegetarian,
      onlyVegan,
      selectedMeatType,
    ],
  );

  const handleCategoryChange = (event) => {
    setSelectedCategory(event.target.value);
  };

  const handleMaxPriceChange = (event) => {
    setMaxPrice(event.target.value);
  };

  const handleAvailabilityChange = (event) => {
    setOnlyAvailable(event.target.checked);
  };

  const handleVegetarianChange = (event) => {
    setOnlyVegetarian(event.target.checked);
  };

  const handleVeganChange = (event) => {
    setOnlyVegan(event.target.checked);
  };

  const handleMeatTypeChange = (event) => {
    setSelectedMeatType(event.target.value);
  };

  const handleFeedbackRatingChange = (event) => {
    setFeedbackRating(event.target.value);
  };

  const handleFeedbackCommentChange = (event) => {
    setFeedbackComment(event.target.value);
  };

  const handleFeedbackSubmit = (event) => {
    event.preventDefault();

    const feedback = {
      rating: Number(feedbackRating),
      comment: feedbackComment,
    };

    setFeedbackMessage("");

    saveFeedback(feedback)
      .then(() => {
        setFeedbackMessage("Feedback-ul a fost trimis cu succes.");

        setFeedbackRating(INITIAL_FEEDBACK_RATING);

        setFeedbackComment("");
      })
      .catch((error) => {
        console.error("Eroare la trimiterea feedback-ului:", error);

        setFeedbackMessage("Feedback-ul nu a putut fi trimis.");
      });
  };

  return {
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
  };
}

export default useClientMenu;
