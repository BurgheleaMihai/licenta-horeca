import { Fragment } from "react";

import { statusLabels } from "../constants/orderHistoryConstants";
import { formatDateTime, getTableNumber } from "../utils/orderHistoryUtils";

/*
 * Afiseaza comenzile gasite, valoarea lor
 * totala si detaliile fiecarei comenzi.
 */
function OrderHistoryResultsSection({
  loading,
  resultsMode,
  displayedOrders,
  displayedOrdersValue,
  expandedOrderId,
  onToggleDetails,
}) {
  return (
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
          Selecteaza filtrele dorite si apasa „Aplica filtrele” sau foloseste
          butonul „Ultimele 5 comenzi”.
        </p>
      ) : (
        <>
          <div className="admin-order-history-summary">
            <p>
              Comenzi gasite: <strong>{displayedOrders.length}</strong>
            </p>

            <p>
              Valoare totala:{" "}
              <strong>{displayedOrdersValue.toFixed(2)} lei</strong>
            </p>
          </div>

          {displayedOrders.length === 0 ? (
            <p>Nu exista comenzi care respecta filtrele selectate.</p>
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
                    const isExpanded = expandedOrderId === order.id;

                    return (
                      <Fragment key={order.id}>
                        <tr>
                          <td>#{order.id}</td>

                          <td>{formatDateTime(order.createdAt)}</td>

                          <td>{formatDateTime(order.completedAt)}</td>

                          <td>{getTableNumber(order) ?? "Necunoscuta"}</td>

                          <td>
                            <span
                              className={
                                "order-history-status " +
                                `order-history-status-${order.status?.toLowerCase()}`
                              }
                            >
                              {statusLabels[order.status] || order.status}
                            </span>
                          </td>

                          <td>
                            {Number(order.totalPrice ?? 0).toFixed(2)} lei
                          </td>

                          <td>
                            <button
                              type="button"
                              className="order-history-details-button"
                              onClick={() => onToggleDetails(order.id)}
                            >
                              {isExpanded ? "Ascunde" : "Vezi detalii"}
                            </button>
                          </td>
                        </tr>

                        {isExpanded && (
                          <tr className="order-history-details-row">
                            <td colSpan="7">
                              <div className="order-history-details">
                                <div className="order-history-metadata">
                                  <p>
                                    <strong>Cod sesiune:</strong>{" "}
                                    {order.tableSession?.sessionCode ||
                                      "Necunoscut"}
                                  </p>

                                  <p>
                                    <strong>Sesiune inceputa:</strong>{" "}
                                    {formatDateTime(
                                      order.tableSession?.startedAt,
                                    )}
                                  </p>

                                  <p>
                                    <strong>Sesiune inchisa:</strong>{" "}
                                    {formatDateTime(
                                      order.tableSession?.endedAt,
                                    )}
                                  </p>
                                </div>

                                <h3>Produsele comenzii</h3>

                                {!order.items || order.items.length === 0 ? (
                                  <p>Comanda nu contine produse.</p>
                                ) : (
                                  <div className="order-history-items">
                                    {order.items.map((item) => (
                                      <div
                                        key={item.id}
                                        className="order-history-item"
                                      >
                                        <h4>
                                          {item.product?.name ||
                                            "Produs necunoscut"}
                                        </h4>

                                        <p>Cantitate: {item.quantity}</p>

                                        <p>
                                          Pret unitar:{" "}
                                          {Number(item.unitPrice ?? 0).toFixed(
                                            2,
                                          )}{" "}
                                          lei
                                        </p>

                                        <p>
                                          Subtotal:{" "}
                                          {Number(item.subtotal ?? 0).toFixed(
                                            2,
                                          )}{" "}
                                          lei
                                        </p>

                                        <p>
                                          Status:{" "}
                                          {statusLabels[item.status] ||
                                            item.status}
                                        </p>
                                      </div>
                                    ))}
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
  );
}

export default OrderHistoryResultsSection;
