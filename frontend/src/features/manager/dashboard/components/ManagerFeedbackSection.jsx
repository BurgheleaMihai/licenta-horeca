/*
 * Afiseaza feedback-ul oferit de clienti.
 */
function ManagerFeedbackSection({ feedbackList }) {
  return (
    <section className="manager-section">
      <h2>Feedback clienti</h2>

      {feedbackList.length === 0 ? (
        <p>Nu exista feedback salvat.</p>
      ) : (
        <div className="manager-grid">
          {feedbackList.map((feedback) => (
            <div key={feedback.id} className="manager-card">
              <p>Rating: {feedback.rating} / 5</p>

              <p>Comentariu: {feedback.comment || "Fara comentariu"}</p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default ManagerFeedbackSection;
