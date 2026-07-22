import {
  Fragment,
  useEffect,
  useMemo,
  useState,
} from "react";
import { getAllOrders } from "../api/orderApi";

const statusLabels = {
  NOUA: "Noua",
  IN_PREPARARE: "In preparare",
  GATA: "Gata",
  SERVITA: "Servita",
  ANULATA: "Anulata",
};

const initialFilters = {
  startDate: "",
  endDate: "",
  startTime: "00:00",
  endTime: "23:59",
  status: "ALL_CLOSED",
  tableNumber: "ALL",
  minValue: "",
  maxValue: "",
};

const formatDateTime = (value) => {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return date.toLocaleString("ro-RO");
};

const getTableNumber = (order) => {
  return (
    order.tableSession?.restaurantTable?.tableNumber ??
    null
  );
};

const isClosedOrder = (order) => {
  return (
    order.status === "SERVITA" ||
    order.status === "ANULATA"
  );
};

function AdminOrderHistoryPage() {
  const [orders, setOrders] = useState([]);

  const [filters, setFilters] = useState({
    ...initialFilters,
  });

  const [appliedFilters, setAppliedFilters] =
    useState({
      ...initialFilters,
    });

  /*
   * NONE = nu sunt afisate rezultate
   * FILTERED = sunt afisate rezultatele filtrarii
   * RECENT = sunt afisate ultimele 5 comenzi inchise
   */
  const [resultsMode, setResultsMode] =
    useState("NONE");

  const [expandedOrderId, setExpandedOrderId] =
    useState(null);

  const [loading, setLoading] = useState(true);

  const [errorMessage, setErrorMessage] =
    useState("");

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = () => {
    setLoading(true);
    setErrorMessage("");

    getAllOrders()
      .then((response) => {
        setOrders(response.data);
      })
      .catch((error) => {
        console.error(
          "Eroare la incarcarea istoricului comenzilor:",
          error
        );

        setErrorMessage(
          "Istoricul comenzilor nu a putut fi incarcat."
        );
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const tableNumbers = useMemo(() => {
    return [
      ...new Set(
        orders
          .filter(isClosedOrder)
          .map((order) => getTableNumber(order))
          .filter(
            (tableNumber) => tableNumber !== null
          )
      ),
    ].sort((firstNumber, secondNumber) => {
      return firstNumber - secondNumber;
    });
  }, [orders]);

  const handleFilterChange = (
    fieldName,
    fieldValue
  ) => {
    setFilters((currentFilters) => ({
      ...currentFilters,
      [fieldName]: fieldValue,
    }));
  };

  const handleFilterSubmit = (event) => {
    event.preventDefault();

    setErrorMessage("");
    setExpandedOrderId(null);

    const hasOnlyOneDate =
      Boolean(filters.startDate) !==
      Boolean(filters.endDate);

    if (hasOnlyOneDate) {
      setErrorMessage(
        "Completeaza atat data de inceput, cat si data de sfarsit."
      );

      return;
    }

    if (
      filters.startDate &&
      filters.endDate
    ) {
      const startDateTime = new Date(
        `${filters.startDate}T${filters.startTime}:00`
      );

      const endDateTime = new Date(
        `${filters.endDate}T${filters.endTime}:59.999`
      );

      if (
        Number.isNaN(startDateTime.getTime()) ||
        Number.isNaN(endDateTime.getTime())
      ) {
        setErrorMessage(
          "Intervalul selectat nu este valid."
        );

        return;
      }

      if (endDateTime < startDateTime) {
        setErrorMessage(
          "Sfarsitul intervalului trebuie sa fie dupa inceput."
        );

        return;
      }
    }

    const minimumValue =
      filters.minValue === ""
        ? null
        : Number(filters.minValue);

    const maximumValue =
      filters.maxValue === ""
        ? null
        : Number(filters.maxValue);

    if (
      minimumValue !== null &&
      (Number.isNaN(minimumValue) ||
        minimumValue < 0)
    ) {
      setErrorMessage(
        "Valoarea minima trebuie sa fie zero sau mai mare."
      );

      return;
    }

    if (
      maximumValue !== null &&
      (Number.isNaN(maximumValue) ||
        maximumValue < 0)
    ) {
      setErrorMessage(
        "Valoarea maxima trebuie sa fie zero sau mai mare."
      );

      return;
    }

    if (
      minimumValue !== null &&
      maximumValue !== null &&
      maximumValue < minimumValue
    ) {
      setErrorMessage(
        "Valoarea maxima trebuie sa fie mai mare decat valoarea minima."
      );

      return;
    }

    setAppliedFilters({
      ...filters,
    });

    setResultsMode("FILTERED");
  };

  const handleShowRecentOrders = () => {
    setErrorMessage("");
    setExpandedOrderId(null);
    setResultsMode("RECENT");
  };

  const handleResetFilters = () => {
    setFilters({
      ...initialFilters,
    });

    setAppliedFilters({
      ...initialFilters,
    });

    setExpandedOrderId(null);
    setResultsMode("NONE");
    setErrorMessage("");
  };

  const sortedClosedOrders = useMemo(() => {
    return orders
      .filter(isClosedOrder)
      .sort((firstOrder, secondOrder) => {
        return (
          new Date(secondOrder.createdAt).getTime() -
          new Date(firstOrder.createdAt).getTime()
        );
      });
  }, [orders]);

  const filteredOrders = useMemo(() => {
    let startDateTime = null;
    let endDateTime = null;

    if (
      appliedFilters.startDate &&
      appliedFilters.endDate
    ) {
      startDateTime = new Date(
        `${appliedFilters.startDate}T${appliedFilters.startTime}:00`
      );

      endDateTime = new Date(
        `${appliedFilters.endDate}T${appliedFilters.endTime}:59.999`
      );
    }

    const minimumValue =
      appliedFilters.minValue === ""
        ? null
        : Number(appliedFilters.minValue);

    const maximumValue =
      appliedFilters.maxValue === ""
        ? null
        : Number(appliedFilters.maxValue);

    return sortedClosedOrders.filter((order) => {
      if (
        appliedFilters.status !== "ALL_CLOSED" &&
        order.status !== appliedFilters.status
      ) {
        return false;
      }

      const tableNumber =
        getTableNumber(order);

      if (
        appliedFilters.tableNumber !== "ALL" &&
        String(tableNumber) !==
          appliedFilters.tableNumber
      ) {
        return false;
      }

      const orderValue = Number(
        order.totalPrice ?? 0
      );

      if (
        minimumValue !== null &&
        orderValue < minimumValue
      ) {
        return false;
      }

      if (
        maximumValue !== null &&
        orderValue > maximumValue
      ) {
        return false;
      }

      if (
        startDateTime &&
        endDateTime
      ) {
        const createdAt = new Date(
          order.createdAt
        );

        if (
          Number.isNaN(createdAt.getTime()) ||
          createdAt < startDateTime ||
          createdAt > endDateTime
        ) {
          return false;
        }
      }

      return true;
    });
  }, [
    sortedClosedOrders,
    appliedFilters,
  ]);

  const displayedOrders = useMemo(() => {
    if (resultsMode === "RECENT") {
      return sortedClosedOrders.slice(0, 5);
    }

    if (resultsMode === "FILTERED") {
      return filteredOrders;
    }

    return [];
  }, [
    resultsMode,
    sortedClosedOrders,
    filteredOrders,
  ]);

  const displayedOrdersValue = useMemo(() => {
    return displayedOrders.reduce(
      (sum, order) => {
        return (
          sum + Number(order.totalPrice ?? 0)
        );
      },
      0
    );
  }, [displayedOrders]);

  const handleToggleDetails = (orderId) => {
    setExpandedOrderId((currentOrderId) => {
      return currentOrderId === orderId
        ? null
        : orderId;
    });
  };

  const handleBackToAdmin = () => {
    globalThis.location.href = "/admin";
  };

  const handleOpenStatistics = () => {
    globalThis.location.href =
      "/admin/statistics";
  };

  const handleLogout = () => {
    localStorage.removeItem("user");
    globalThis.location.href = "/login";
  };

  return (
    <div className="admin-order-history-page">
      <header className="admin-order-history-header">
        <h1>Istoric comenzi</h1>

        <p>
          Cauta comenzile servite sau anulate dupa
          perioada, status, masa sau valoare.
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
            className="admin-nav-button"
            onClick={handleOpenStatistics}
          >
            Statistici si rapoarte
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
        <p className="error-message admin-order-history-message">
          {errorMessage}
        </p>
      )}

      <section className="admin-order-history-section">
        <h2>Cauta in istoric</h2>

        <form
          className="admin-order-history-filter"
          onSubmit={handleFilterSubmit}
        >
          <div className="admin-order-history-fields">
            <div className="filter-group">
              <label htmlFor="history-start-date">
                Data de inceput
              </label>

              <input
                id="history-start-date"
                type="date"
                value={filters.startDate}
                onChange={(event) =>
                  handleFilterChange(
                    "startDate",
                    event.target.value
                  )
                }
              />
            </div>

            <div className="filter-group">
              <label htmlFor="history-end-date">
                Data de sfarsit
              </label>

              <input
                id="history-end-date"
                type="date"
                value={filters.endDate}
                onChange={(event) =>
                  handleFilterChange(
                    "endDate",
                    event.target.value
                  )
                }
              />
            </div>

            <div className="filter-group">
              <label htmlFor="history-start-time">
                Ora de inceput
              </label>

              <input
                id="history-start-time"
                type="time"
                value={filters.startTime}
                onChange={(event) =>
                  handleFilterChange(
                    "startTime",
                    event.target.value
                  )
                }
              />
            </div>

            <div className="filter-group">
              <label htmlFor="history-end-time">
                Ora de sfarsit
              </label>

              <input
                id="history-end-time"
                type="time"
                value={filters.endTime}
                onChange={(event) =>
                  handleFilterChange(
                    "endTime",
                    event.target.value
                  )
                }
              />
            </div>

            <div className="filter-group">
              <label htmlFor="history-status">
                Status
              </label>

              <select
                id="history-status"
                value={filters.status}
                onChange={(event) =>
                  handleFilterChange(
                    "status",
                    event.target.value
                  )
                }
              >
                <option value="ALL_CLOSED">
                  Servite si anulate
                </option>

                <option value="SERVITA">
                  Servita
                </option>

                <option value="ANULATA">
                  Anulata
                </option>
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="history-table">
                Masa
              </label>

              <select
                id="history-table"
                value={filters.tableNumber}
                onChange={(event) =>
                  handleFilterChange(
                    "tableNumber",
                    event.target.value
                  )
                }
              >
                <option value="ALL">
                  Toate mesele
                </option>

                {tableNumbers.map((tableNumber) => (
                  <option
                    key={tableNumber}
                    value={String(tableNumber)}
                  >
                    Masa {tableNumber}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label htmlFor="history-min-value">
                Valoare minima
              </label>

              <input
                id="history-min-value"
                type="number"
                min="0"
                step="0.01"
                value={filters.minValue}
                placeholder="0 lei"
                onChange={(event) =>
                  handleFilterChange(
                    "minValue",
                    event.target.value
                  )
                }
              />
            </div>

            <div className="filter-group">
              <label htmlFor="history-max-value">
                Valoare maxima
              </label>

              <input
                id="history-max-value"
                type="number"
                min="0"
                step="0.01"
                value={filters.maxValue}
                placeholder="Fara limita"
                onChange={(event) =>
                  handleFilterChange(
                    "maxValue",
                    event.target.value
                  )
                }
              />
            </div>
          </div>

          <div className="admin-order-history-actions">
            <button
              type="submit"
              className="history-primary-button"
            >
              Aplica filtrele
            </button>

            <button
              type="button"
              className="history-primary-button"
              onClick={handleShowRecentOrders}
            >
              Ultimele 5 comenzi
            </button>

            <button
              type="button"
              className="history-secondary-button"
              onClick={handleResetFilters}
            >
              Reseteaza
            </button>

            <button
              type="button"
              className="history-secondary-button"
              onClick={loadOrders}
              disabled={loading}
            >
              {loading
                ? "Se incarca..."
                : "Reincarca datele"}
            </button>
          </div>
        </form>
      </section>

      <section className="admin-order-history-section">
        <h2>
          {resultsMode === "RECENT"
            ? "Ultimele 5 comenzi"
            : "Rezultatele cautarii"}
        </h2>

        {loading ? (
          <p>Se incarca istoricul...</p>
        ) : resultsMode === "NONE" ? (
          <p className="admin-order-history-empty">
            Selecteaza filtrele dorite si apasa
            „Aplica filtrele” sau foloseste butonul
            „Ultimele 5 comenzi”.
          </p>
        ) : (
          <>
            <div className="admin-order-history-summary">
              <p>
                Comenzi gasite:{" "}
                <strong>
                  {displayedOrders.length}
                </strong>
              </p>

              <p>
                Valoare totala:{" "}
                <strong>
                  {displayedOrdersValue.toFixed(2)} lei
                </strong>
              </p>
            </div>

            {displayedOrders.length === 0 ? (
              <p>
                Nu exista comenzi care respecta
                filtrele selectate.
              </p>
            ) : (
              <div className="admin-order-history-table-wrapper">
                <table className="admin-order-history-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Creata la</th>
                      <th>Finalizata la</th>
                      <th>Masa</th>
                      <th>Status</th>
                      <th>Total</th>
                      <th>Detalii</th>
                    </tr>
                  </thead>

                  <tbody>
                    {displayedOrders.map((order) => {
                      const isExpanded =
                        expandedOrderId === order.id;

                      return (
                        <Fragment key={order.id}>
                          <tr>
                            <td>#{order.id}</td>

                            <td>
                              {formatDateTime(
                                order.createdAt
                              )}
                            </td>

                            <td>
                              {formatDateTime(
                                order.completedAt
                              )}
                            </td>

                            <td>
                              {getTableNumber(order) ??
                                "Necunoscuta"}
                            </td>

                            <td>
                              <span
                                className={`order-history-status order-history-status-${order.status?.toLowerCase()}`}
                              >
                                {statusLabels[
                                  order.status
                                ] || order.status}
                              </span>
                            </td>

                            <td>
                              {Number(
                                order.totalPrice ?? 0
                              ).toFixed(2)}{" "}
                              lei
                            </td>

                            <td>
                              <button
                                type="button"
                                className="order-history-details-button"
                                onClick={() =>
                                  handleToggleDetails(
                                    order.id
                                  )
                                }
                              >
                                {isExpanded
                                  ? "Ascunde"
                                  : "Vezi detalii"}
                              </button>
                            </td>
                          </tr>

                          {isExpanded && (
                            <tr className="order-history-details-row">
                              <td colSpan="7">
                                <div className="order-history-details">
                                  <div className="order-history-metadata">
                                    <p>
                                      <strong>
                                        Cod sesiune:
                                      </strong>{" "}
                                      {order.tableSession
                                        ?.sessionCode ||
                                        "Necunoscut"}
                                    </p>

                                    <p>
                                      <strong>
                                        Sesiune inceputa:
                                      </strong>{" "}
                                      {formatDateTime(
                                        order.tableSession
                                          ?.startedAt
                                      )}
                                    </p>

                                    <p>
                                      <strong>
                                        Sesiune inchisa:
                                      </strong>{" "}
                                      {formatDateTime(
                                        order.tableSession
                                          ?.endedAt
                                      )}
                                    </p>
                                  </div>

                                  <h3>
                                    Produsele comenzii
                                  </h3>

                                  {!order.items ||
                                  order.items.length ===
                                    0 ? (
                                    <p>
                                      Comanda nu contine
                                      produse.
                                    </p>
                                  ) : (
                                    <div className="order-history-items">
                                      {order.items.map(
                                        (item) => (
                                          <div
                                            key={item.id}
                                            className="order-history-item"
                                          >
                                            <h4>
                                              {item.product
                                                ?.name ||
                                                "Produs necunoscut"}
                                            </h4>

                                            <p>
                                              Cantitate:{" "}
                                              {item.quantity}
                                            </p>

                                            <p>
                                              Pret unitar:{" "}
                                              {Number(
                                                item.unitPrice ??
                                                  0
                                              ).toFixed(
                                                2
                                              )}{" "}
                                              lei
                                            </p>

                                            <p>
                                              Subtotal:{" "}
                                              {Number(
                                                item.subtotal ??
                                                  0
                                              ).toFixed(
                                                2
                                              )}{" "}
                                              lei
                                            </p>

                                            <p>
                                              Status:{" "}
                                              {statusLabels[
                                                item.status
                                              ] ||
                                                item.status}
                                            </p>
                                          </div>
                                        )
                                      )}
                                    </div>
                                  )}
                                </div>
                              </td>
                            </tr>
                          )}
                        </Fragment>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}

export default AdminOrderHistoryPage;