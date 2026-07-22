import { useEffect, useState } from "react";
import {
  getActiveOrders,
  getAllOrders,
  getOrderStatistics,
} from "../api/orderApi";
import { getAllFeedback } from "../api/productApi";
import { getTrafficSummary } from "../api/trafficApi";

const getCurrentDateValue = () => {
  const currentDate = new Date();

  const timezoneOffset = currentDate.getTimezoneOffset() * 60 * 1000;

  return new Date(currentDate.getTime() - timezoneOffset)
    .toISOString()
    .split("T")[0];
};

function ManagerPage() {
  const initialDate = getCurrentDateValue();

  const [activeOrders, setActiveOrders] = useState([]);
  const [allOrders, setAllOrders] = useState([]);
  const [feedbackList, setFeedbackList] = useState([]);

  const [todayStatistics, setTodayStatistics] = useState({
    activeOrders: 0,
    servedOrders: 0,
    sales: 0,
    averageRating: 0,
  });

  const [selectedDate, setSelectedDate] = useState(initialDate);

  const [startTime, setStartTime] = useState("00:00");

  const [endTime, setEndTime] = useState("23:59");

  const [appliedFilters, setAppliedFilters] = useState({
    date: initialDate,
    startTime: "00:00",
    endTime: "23:59",
  });

  const [statisticsLoading, setStatisticsLoading] = useState(false);

  const [trafficSummary, setTrafficSummary] = useState({
    entries: 0,
    exits: 0,
    estimatedOccupancy: 0,
  });

  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadManagerData();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  const handleOpenStocks = () => {
    globalThis.location.href = "/manager-supplies";
  };

  const loadStatistics = (date, intervalStart, intervalEnd) => {
    setStatisticsLoading(true);

    return getOrderStatistics(date, intervalStart, intervalEnd)
      .then((response) => {
        setTodayStatistics(response.data);

        setAppliedFilters({
          date,
          startTime: intervalStart,
          endTime: intervalEnd,
        });
      })
      .catch((error) => {
        console.error("Eroare la incarcarea statisticilor:", error);

        setErrorMessage(
          error.response?.data?.message ||
            "Statisticile nu au putut fi incarcate.",
        );
      })
      .finally(() => {
        setStatisticsLoading(false);
      });
  };

  const loadManagerData = () => {
    setErrorMessage("");

    loadStatistics(initialDate, "00:00", "23:59");

    getActiveOrders()
      .then((response) => {
        setActiveOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor active:", error);

        setErrorMessage("Comenzile active nu au putut fi incarcate.");
      });

    getAllOrders()
      .then((response) => {
        setAllOrders(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea comenzilor:", error);

        setErrorMessage("Toate comenzile nu au putut fi incarcate.");
      });

    getAllFeedback()
      .then((response) => {
        setFeedbackList(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea feedback-ului:", error);

        setErrorMessage("Feedback-ul nu a putut fi incarcat.");
      });

    getTrafficSummary()
      .then((response) => {
        setTrafficSummary(response.data);
      })
      .catch((error) => {
        console.error("Eroare la incarcarea datelor senzorilor:", error);

        setErrorMessage("Datele senzorilor nu au putut fi incarcate.");
      });
  };

  const handleStatisticsSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");

    if (!selectedDate || !startTime || !endTime) {
      setErrorMessage("Completeaza data si intervalul orar.");

      return;
    }

    if (endTime < startTime) {
      setErrorMessage("Ora de sfarsit trebuie sa fie dupa ora de inceput.");

      return;
    }

    loadStatistics(selectedDate, startTime, endTime);
  };

  const handleResetStatistics = () => {
    const currentDate = getCurrentDateValue();

    setSelectedDate(currentDate);
    setStartTime("00:00");
    setEndTime("23:59");
    setErrorMessage("");

    loadStatistics(currentDate, "00:00", "23:59");
  };

  const allServedOrders = allOrders.filter(
    (order) => order.status === "SERVITA",
  );

  const newOrdersCount = activeOrders.filter(
    (order) => order.status === "NOUA",
  ).length;

  const inPreparationOrdersCount = activeOrders.filter(
    (order) => order.status === "IN_PREPARARE",
  ).length;

  const readyOrdersCount = activeOrders.filter(
    (order) => order.status === "GATA",
  ).length;

  const productSales = {};

  allServedOrders.forEach((order) => {
    order.items?.forEach((item) => {
      const productName = item.product?.name || "Produs necunoscut";

      if (!productSales[productName]) {
        productSales[productName] = 0;
      }

      productSales[productName] += Number(item.quantity);
    });
  });

  const productSalesList = Object.entries(productSales)
    .map(([name, quantity]) => ({
      name,
      quantity,
    }))
    .sort(
      (firstProduct, secondProduct) =>
        secondProduct.quantity - firstProduct.quantity,
    );

  return (
    <div className="manager-page">
      <header className="manager-header">
        <h1>Panou manager</h1>

        <div className="manager-header-actions">
          <button
            type="button"
            className="manager-nav-button"
            onClick={handleOpenStocks}
          >
            Stocuri
          </button>

          <button
            type="button"
            className="logout-button"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      </header>

      {errorMessage && (
        <p className="error-message manager-page-message">{errorMessage}</p>
      )}

      <section className="manager-section">
        <h2>Indicatori pe perioada selectata</h2>

        <form
          className="manager-statistics-filter"
          onSubmit={handleStatisticsSubmit}
        >
          <div className="filter-group">
            <label htmlFor="statistics-date">Data</label>

            <input
              id="statistics-date"
              type="date"
              value={selectedDate}
              onChange={(event) => {
                setSelectedDate(event.target.value);
              }}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="statistics-start-time">Ora de inceput</label>

            <input
              id="statistics-start-time"
              type="time"
              value={startTime}
              onChange={(event) => {
                setStartTime(event.target.value);
              }}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="statistics-end-time">Ora de sfarsit</label>

            <input
              id="statistics-end-time"
              type="time"
              value={endTime}
              onChange={(event) => {
                setEndTime(event.target.value);
              }}
            />
          </div>

          <div className="manager-filter-actions">
            <button type="submit" disabled={statisticsLoading}>
              {statisticsLoading ? "Se incarca..." : "Aplica filtrul"}
            </button>

            <button
              type="button"
              className="secondary-button"
              onClick={handleResetStatistics}
              disabled={statisticsLoading}
            >
              Astazi
            </button>
          </div>
        </form>

        <p className="manager-filter-summary">
          Data: <strong>{appliedFilters.date}</strong>, interval:{" "}
          <strong>
            {appliedFilters.startTime} - {appliedFilters.endTime}
          </strong>
        </p>

        <p className="manager-filter-note">
          Comenzile active reprezinta situatia curenta. Comenzile servite,
          vanzarile si ratingul sunt calculate pentru intervalul selectat.
        </p>

        <div className="manager-grid">
          <div className="manager-card">
            <h3>Comenzi active acum</h3>

            <p>{todayStatistics.activeOrders}</p>
          </div>

          <div className="manager-card">
            <h3>Comenzi servite</h3>

            <p>{todayStatistics.servedOrders}</p>
          </div>

          <div className="manager-card">
            <h3>Vanzari</h3>

            <p>{Number(todayStatistics.sales ?? 0).toFixed(2)} lei</p>
          </div>

          <div className="manager-card">
            <h3>Rating mediu</h3>

            <p>{Number(todayStatistics.averageRating ?? 0).toFixed(2)} / 5</p>
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

            <p>{trafficSummary.estimatedOccupancy} mese</p>
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
        <h2>Recomandari pentru stocul de produse</h2>

        {productSalesList.length === 0 ? (
          <p>Nu exista suficiente date pentru recomandari.</p>
        ) : (
          <div className="manager-grid">
            {productSalesList.slice(0, 3).map((product) => (
              <div key={product.name} className="manager-card">
                <h3>{product.name}</h3>

                <p>
                  Produsul a fost vandut de <strong>{product.quantity}</strong>{" "}
                  ori.
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="manager-section">
        <h2>Comenzi active</h2>

        {activeOrders.length === 0 ? (
          <p>Nu exista comenzi active.</p>
        ) : (
          <div className="manager-grid">
            {activeOrders.map((order) => (
              <div key={order.id} className="manager-card">
                <h3>Comanda #{order.id}</h3>

                <p>Status: {order.status}</p>

                <p>Total: {Number(order.totalPrice).toFixed(2)} lei</p>

                <p>
                  Masa:{" "}
                  {order.tableSession?.restaurantTable?.tableNumber ||
                    "necunoscuta"}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

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
    </div>
  );
}

export default ManagerPage;
