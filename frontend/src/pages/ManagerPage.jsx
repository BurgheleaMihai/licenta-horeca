import { useEffect, useState } from "react";
import { getActiveOrders, getAllOrders } from "../api/orderApi";
import { getAllFeedback } from "../api/productApi";
import { getTrafficSummary } from "../api/trafficApi";

function ManagerPage() {
  const [activeOrders, setActiveOrders] = useState([]);
  const [allOrders, setAllOrders] = useState([]);
  const [feedbackList, setFeedbackList] = useState([]);
  const [trafficSummary, setTrafficSummary] = useState({
    entries: 0,
    exits: 0,
    estimatedOccupancy: 0
  });
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadManagerData();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  const loadManagerData = () => {
    getActiveOrders()
      .then((response) => setActiveOrders(response.data))
      .catch(() => setErrorMessage("Comenzile active nu au putut fi incarcate."));

    getAllOrders()
      .then((response) => setAllOrders(response.data))
      .catch(() => setErrorMessage("Toate comenzile nu au putut fi incarcate."));

    getAllFeedback()
      .then((response) => setFeedbackList(response.data))
      .catch(() => setErrorMessage("Feedback-ul nu a putut fi incarcat."));

    getTrafficSummary()
      .then((response) => setTrafficSummary(response.data))
      .catch(() => setErrorMessage("Datele senzorilor nu au putut fi incarcate."));
  };

  const servedOrders = allOrders.filter((order) => order.status === "SERVITA");

  const totalSales = servedOrders.reduce((sum, order) => {
    return sum + Number(order.totalPrice);
  }, 0);

  const averageRating =
    feedbackList.length === 0
      ? 0
      : feedbackList.reduce((sum, feedback) => sum + Number(feedback.rating), 0) /
        feedbackList.length;

  const newOrdersCount = activeOrders.filter(
    (order) => order.status === "NOUA"
  ).length;

  const inPreparationOrdersCount = activeOrders.filter(
    (order) => order.status === "IN_PREPARARE"
  ).length;

  const readyOrdersCount = activeOrders.filter(
    (order) => order.status === "GATA"
  ).length;

  return (
    <div className="manager-page">
      <header className="manager-header">
        <h1>Panou manager</h1>
        <p>Monitorizare comenzi si ocupare restaurant.</p>

        <div className="manager-header-actions">
          <button className="logout-button" onClick={handleLogout}>
            Logout
          </button>

          <button
            className="manager-nav-button"
            onClick={() => window.location.href = "/manager-supplies"}
          >
            Stoc auxiliar
          </button>
        </div>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <section className="manager-section">
        <h2>Indicatori principali</h2>

        <div className="manager-grid">
          <div className="manager-card">
            <h3>Comenzi active</h3>
            <p>{activeOrders.length}</p>
          </div>

          <div className="manager-card">
            <h3>Comenzi servite</h3>
            <p>{servedOrders.length}</p>
          </div>

          <div className="manager-card">
            <h3>Total vanzari</h3>
            <p>{totalSales.toFixed(2)} lei</p>
          </div>

          <div className="manager-card">
            <h3>Rating mediu</h3>
            <p>{averageRating.toFixed(2)} / 5</p>
          </div>
        </div>
      </section>

      <section className="manager-section">
        <h2>Senzori intrare-iesire</h2>

        <div className="manager-grid">
          <div className="manager-card">
            <h3>Intrari</h3>
            <p>{trafficSummary.entries}</p>
          </div>

          <div className="manager-card">
            <h3>Iesiri</h3>
            <p>{trafficSummary.exits}</p>
          </div>

          <div className="manager-card">
            <h3>Ocupare estimata</h3>
            <p>{trafficSummary.estimatedOccupancy} clienti</p>
          </div>
        </div>
      </section>

      <section className="manager-section">
        <h2>Status operational pe zone</h2>

        <div className="manager-grid">
          <div className="manager-card">
            <h3>Ospatar - comenzi noi</h3>
            <p>{newOrdersCount}</p>
          </div>

          <div className="manager-card">
            <h3>Bucatarie/Bar - in preparare</h3>
            <p>{inPreparationOrdersCount}</p>
          </div>

          <div className="manager-card">
            <h3>Ospatar - comenzi gata</h3>
            <p>{readyOrdersCount}</p>
          </div>
        </div>
      </section>

      <section className="manager-section">
        <h2>Comenzi active</h2>

        {activeOrders.length === 0 ? (
          <p>Nu exista comenzi active.</p>
        ) : (
          activeOrders.map((order) => (
            <div key={order.id} className="manager-card">
              <h3>Comanda #{order.id}</h3>
              <p>Status: {order.status}</p>
              <p>Total: {Number(order.totalPrice).toFixed(2)} lei</p>
              <p>
                Masa:{" "}
                {order.tableSession?.restaurantTable?.tableNumber || "necunoscuta"}
              </p>
            </div>
          ))
        )}
      </section>

      <section className="manager-section">
        <h2>Feedback clienti</h2>

        {feedbackList.length === 0 ? (
          <p>Nu exista feedback salvat.</p>
        ) : (
          feedbackList.map((feedback) => (
            <div key={feedback.id} className="manager-card">
              <p>Rating: {feedback.rating} / 5</p>
              <p>Comentariu: {feedback.comment || "Fara comentariu"}</p>
            </div>
          ))
        )}
      </section>
    </div>
  );
}

export default ManagerPage;