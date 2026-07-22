import { useEffect, useMemo, useState } from "react";
import { getAllOrders } from "../api/orderApi";
import { getAllFeedback } from "../api/productApi";

const getLocalDateValue = (date) => {
  const timezoneOffset =
    date.getTimezoneOffset() * 60 * 1000;

  return new Date(date.getTime() - timezoneOffset)
    .toISOString()
    .split("T")[0];
};

const getMinutesFromTime = (timeValue) => {
  const [hours, minutes] = timeValue
    .split(":")
    .map(Number);

  return hours * 60 + minutes;
};

const getPeriodDateRange = (filters) => {
  const today = new Date();

  if (filters.period === "TODAY") {
    const todayValue = getLocalDateValue(today);

    return {
      startDate: todayValue,
      endDate: todayValue,
    };
  }

  if (filters.period === "LAST_7_DAYS") {
    const startDate = new Date(today);

    startDate.setDate(today.getDate() - 6);

    return {
      startDate: getLocalDateValue(startDate),
      endDate: getLocalDateValue(today),
    };
  }

  if (filters.period === "CURRENT_MONTH") {
    const firstDayOfMonth = new Date(
      today.getFullYear(),
      today.getMonth(),
      1
    );

    return {
      startDate: getLocalDateValue(firstDayOfMonth),
      endDate: getLocalDateValue(today),
    };
  }

  return {
    startDate: filters.customStartDate,
    endDate: filters.customEndDate,
  };
};

const isTimestampInFilters = (
  timestamp,
  filters
) => {
  if (!timestamp) {
    return false;
  }

  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return false;
  }

  const dateValue = getLocalDateValue(date);

  const { startDate, endDate } =
    getPeriodDateRange(filters);

  if (
    dateValue < startDate ||
    dateValue > endDate
  ) {
    return false;
  }

  const timestampMinutes =
    date.getHours() * 60 + date.getMinutes();

  const startMinutes = getMinutesFromTime(
    filters.startTime
  );

  const endMinutes = getMinutesFromTime(
    filters.endTime
  );

  return (
    timestampMinutes >= startMinutes &&
    timestampMinutes <= endMinutes
  );
};

const getPeriodLabel = (filters) => {
  if (filters.period === "TODAY") {
    return "Astazi";
  }

  if (filters.period === "LAST_7_DAYS") {
    return "Ultimele 7 zile";
  }

  if (filters.period === "CURRENT_MONTH") {
    return "Luna curenta";
  }

  return (
    `${filters.customStartDate} - ` +
    `${filters.customEndDate}`
  );
};

function AdminStatisticsPage() {
  const todayValue = getLocalDateValue(new Date());

  const [orders, setOrders] = useState([]);
  const [feedbackList, setFeedbackList] =
    useState([]);

  const [period, setPeriod] = useState("TODAY");

  const [customStartDate, setCustomStartDate] =
    useState(todayValue);

  const [customEndDate, setCustomEndDate] =
    useState(todayValue);

  const [startTime, setStartTime] =
    useState("00:00");

  const [endTime, setEndTime] =
    useState("23:59");

  const [appliedFilters, setAppliedFilters] =
    useState({
      period: "TODAY",
      customStartDate: todayValue,
      customEndDate: todayValue,
      startTime: "00:00",
      endTime: "23:59",
    });

  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] =
    useState("");

  useEffect(() => {
    loadStatisticsData();
  }, []);

  const loadStatisticsData = () => {
    setLoading(true);
    setErrorMessage("");

    Promise.all([
      getAllOrders(),
      getAllFeedback(),
    ])
      .then(
        ([
          ordersResponse,
          feedbackResponse,
        ]) => {
          setOrders(ordersResponse.data);
          setFeedbackList(feedbackResponse.data);
        }
      )
      .catch((error) => {
        console.error(
          "Eroare la incarcarea statisticilor:",
          error
        );

        setErrorMessage(
          "Datele pentru statistici nu au putut fi incarcate."
        );
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  const handleFilterSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");

    if (
      period === "CUSTOM" &&
      (!customStartDate || !customEndDate)
    ) {
      setErrorMessage(
        "Selecteaza data de inceput si data de sfarsit."
      );

      return;
    }

    if (
      period === "CUSTOM" &&
      customEndDate < customStartDate
    ) {
      setErrorMessage(
        "Data de sfarsit trebuie sa fie dupa data de inceput."
      );

      return;
    }

    if (endTime < startTime) {
      setErrorMessage(
        "Ora de sfarsit trebuie sa fie dupa ora de inceput."
      );

      return;
    }

    setAppliedFilters({
      period,
      customStartDate,
      customEndDate,
      startTime,
      endTime,
    });
  };

  const handleResetFilters = () => {
    const currentDate =
      getLocalDateValue(new Date());

    setPeriod("TODAY");
    setCustomStartDate(currentDate);
    setCustomEndDate(currentDate);
    setStartTime("00:00");
    setEndTime("23:59");
    setErrorMessage("");

    setAppliedFilters({
      period: "TODAY",
      customStartDate: currentDate,
      customEndDate: currentDate,
      startTime: "00:00",
      endTime: "23:59",
    });
  };

  const statistics = useMemo(() => {
    const filteredOrders = orders.filter(
      (order) =>
        isTimestampInFilters(
          order.createdAt,
          appliedFilters
        )
    );

    const servedOrders = orders.filter(
      (order) =>
        order.status === "SERVITA" &&
        isTimestampInFilters(
          order.completedAt || order.createdAt,
          appliedFilters
        )
    );

    const cancelledOrders =
      filteredOrders.filter(
        (order) => order.status === "ANULATA"
      );

    const filteredFeedback =
      feedbackList.filter((feedback) =>
        isTimestampInFilters(
          feedback.createdAt,
          appliedFilters
        )
      );

    const sales = servedOrders.reduce(
      (sum, order) =>
        sum + Number(order.totalPrice || 0),
      0
    );

    const averageOrderValue =
      servedOrders.length === 0
        ? 0
        : sales / servedOrders.length;

    const averageRating =
      filteredFeedback.length === 0
        ? 0
        : filteredFeedback.reduce(
            (sum, feedback) =>
              sum + Number(feedback.rating || 0),
            0
          ) / filteredFeedback.length;

    const productSales = {};

    servedOrders.forEach((order) => {
      order.items?.forEach((item) => {
        const productName =
          item.product?.name ||
          "Produs necunoscut";

        if (!productSales[productName]) {
          productSales[productName] = 0;
        }

        productSales[productName] += Number(
          item.quantity || 0
        );
      });
    });

    const productSalesList =
      Object.entries(productSales)
        .map(([name, quantity]) => ({
          name,
          quantity,
        }))
        .sort(
          (firstProduct, secondProduct) =>
            secondProduct.quantity -
            firstProduct.quantity
        );

    const totalProductsSold =
      productSalesList.reduce(
        (sum, product) =>
          sum + product.quantity,
        0
      );

    return {
      totalOrders: filteredOrders.length,
      servedOrders: servedOrders.length,
      cancelledOrders:
        cancelledOrders.length,
      sales,
      averageOrderValue,
      averageRating,
      totalProductsSold,
      productSalesList,
    };
  }, [
    orders,
    feedbackList,
    appliedFilters,
  ]);

  return (
    <div className="admin-statistics-page">
      <header className="admin-statistics-header">
        <h1>Statistici si rapoarte</h1>

        <p>
          Analiza comenzilor, vanzarilor si
          feedback-ului pe perioade.
        </p>

        <div className="admin-header-actions">
          <button
            type="button"
            className="admin-nav-button"
            onClick={handleBackToAdmin}
          >
            Inapoi la administrare
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
        <p className="error-message admin-statistics-message">
          {errorMessage}
        </p>
      )}

      <section className="admin-statistics-section">
        <h2>Filtre statistice</h2>

        <form
          className="admin-statistics-filter"
          onSubmit={handleFilterSubmit}
        >
          <div className="filter-group">
            <label htmlFor="statistics-period">
              Perioada
            </label>

            <select
              id="statistics-period"
              value={period}
              onChange={(event) =>
                setPeriod(event.target.value)
              }
            >
              <option value="TODAY">
                Astazi
              </option>

              <option value="LAST_7_DAYS">
                Ultimele 7 zile
              </option>

              <option value="CURRENT_MONTH">
                Luna curenta
              </option>

              <option value="CUSTOM">
                Interval personalizat
              </option>
            </select>
          </div>

          {period === "CUSTOM" && (
            <>
              <div className="filter-group">
                <label htmlFor="custom-start-date">
                  Data de inceput
                </label>

                <input
                  id="custom-start-date"
                  type="date"
                  value={customStartDate}
                  onChange={(event) =>
                    setCustomStartDate(
                      event.target.value
                    )
                  }
                />
              </div>

              <div className="filter-group">
                <label htmlFor="custom-end-date">
                  Data de sfarsit
                </label>

                <input
                  id="custom-end-date"
                  type="date"
                  value={customEndDate}
                  onChange={(event) =>
                    setCustomEndDate(
                      event.target.value
                    )
                  }
                />
              </div>
            </>
          )}

          <div className="filter-group">
            <label htmlFor="admin-start-time">
              Ora de inceput
            </label>

            <input
              id="admin-start-time"
              type="time"
              value={startTime}
              onChange={(event) =>
                setStartTime(event.target.value)
              }
            />
          </div>

          <div className="filter-group">
            <label htmlFor="admin-end-time">
              Ora de sfarsit
            </label>

            <input
              id="admin-end-time"
              type="time"
              value={endTime}
              onChange={(event) =>
                setEndTime(event.target.value)
              }
            />
          </div>

          <div className="admin-statistics-actions">
            <button type="submit">
              Aplica filtrul
            </button>

            <button
              type="button"
              className="secondary-button"
              onClick={handleResetFilters}
            >
              Reseteaza
            </button>
          </div>
        </form>

        <p className="admin-statistics-summary">
          Perioada:{" "}
          <strong>
            {getPeriodLabel(appliedFilters)}
          </strong>
          , interval orar:{" "}
          <strong>
            {appliedFilters.startTime} -{" "}
            {appliedFilters.endTime}
          </strong>
        </p>
      </section>

      <section className="admin-statistics-section">
        <h2>Indicatori principali</h2>

        {loading ? (
          <p>Se incarca statisticile...</p>
        ) : (
          <div className="admin-statistics-grid">
            <div className="admin-statistics-card">
              <h3>Total comenzi</h3>
              <p>{statistics.totalOrders}</p>
            </div>

            <div className="admin-statistics-card">
              <h3>Comenzi servite</h3>
              <p>{statistics.servedOrders}</p>
            </div>

            <div className="admin-statistics-card">
              <h3>Comenzi anulate</h3>
              <p>
                {statistics.cancelledOrders}
              </p>
            </div>

            <div className="admin-statistics-card">
              <h3>Venituri</h3>

              <p>
                {statistics.sales.toFixed(2)} lei
              </p>
            </div>

            <div className="admin-statistics-card">
              <h3>Valoare medie pe comanda</h3>

              <p>
                {statistics.averageOrderValue.toFixed(
                  2
                )}{" "}
                lei
              </p>
            </div>

            <div className="admin-statistics-card">
              <h3>Produse vandute</h3>

              <p>
                {statistics.totalProductsSold}
              </p>
            </div>

            <div className="admin-statistics-card">
              <h3>Rating mediu</h3>

              <p>
                {statistics.averageRating.toFixed(2)}{" "}
                / 5
              </p>
            </div>
          </div>
        )}
      </section>

      <section className="admin-statistics-section">
        <h2>Top produse vandute</h2>

        {loading ? (
          <p>Se incarca produsele...</p>
        ) : statistics.productSalesList.length ===
          0 ? (
          <p>
            Nu exista produse vandute in perioada
            selectata.
          </p>
        ) : (
          <div className="admin-statistics-grid">
            {statistics.productSalesList
              .slice(0, 10)
              .map((product, index) => (
                <div
                  key={product.name}
                  className="admin-statistics-card"
                >
                  <h3>
                    #{index + 1} {product.name}
                  </h3>

                  <p>
                    Cantitate vanduta:{" "}
                    {product.quantity}
                  </p>
                </div>
              ))}
          </div>
        )}
      </section>

      <section className="admin-statistics-section">
        <h2>Observatii despre calcul</h2>

        <p>
          Comenzile totale si comenzile anulate sunt
          filtrate dupa momentul crearii.
        </p>

        <p>
          Comenzile servite si veniturile sunt filtrate
          dupa momentul finalizarii comenzii.
        </p>

        <p>
          Pentru comenzile vechi care nu au completat
          campul de finalizare, este folosita data
          crearii.
        </p>
      </section>
    </div>
  );
}

export default AdminStatisticsPage;