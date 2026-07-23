import { FEEDBACK_RATING_OPTIONS } from "../constants/clientMenuConstants";

function MenuFeedbackSection({
  feedbackRating,
  feedbackComment,
  feedbackMessage,
  onRatingChange,
  onCommentChange,
  onSubmit,
}) {
  return (
    <section className="feedback-section">
      <h2>Spuneți-ne cum a fost experiența</h2>

      <form onSubmit={onSubmit} className="feedback-form">
        <div className="filter-group">
          <label htmlFor="feedback-rating">Rating</label>

          <select
            id="feedback-rating"
            value={feedbackRating}
            onChange={onRatingChange}
          >
            {FEEDBACK_RATING_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="feedback-comment-group">
          <label htmlFor="feedback-comment">Comentariu</label>

          <textarea
            id="feedback-comment"
            value={feedbackComment}
            onChange={onCommentChange}
            placeholder="Scrie un comentariu optional..."
            rows="4"
          />
        </div>

        <button type="submit" className="feedback-button">
          Trimite feedback
        </button>
      </form>

      {feedbackMessage && <p className="feedback-message">{feedbackMessage}</p>}
    </section>
  );
}

export default MenuFeedbackSection;
