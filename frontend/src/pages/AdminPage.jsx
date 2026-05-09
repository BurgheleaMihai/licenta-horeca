import { useEffect, useState } from "react";
import { getAllOrders } from "../api/orderApi";
import { getAllFeedback } from "../api/productApi";
import { getUnavailableAuxiliarySupplies } from "../api/auxiliarySupplyApi";

function AdminPage() {
  const [allOrders, setAllOrders] = useState([]);
  const [feedbackList, setFeedbackList] = useState([]);
  const [periodFilter, setPeriodFilter] = useState("ALL");
  const [errorMessage, setErrorMessage] = useState("");
  const [unavailableSupplies, setUnavailableSupplies] = useState([]);

  useEffect(() => {
    loadAdminData();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");
    window.location.href = "/login";
  };

  const loadAdminData = () => {
    getAllOrders()
      .then((response) => setAllOrders(response.data))
      .catch(() => setErrorMessage("Comenzile nu au putut fi incarcate."));

    getUnavailableAuxiliarySupplies()
      .then((response) => setUnavailableSupplies(response.data))
      .catch(() =>
        setErrorMessage("Produsele auxiliare lipsa nu au putut fi incarcate.")
      );

    getAllFeedback()
      .then((response) => setFeedbackList(response.data))
      .catch(() => setErrorMessage("Feedback-ul nu a putut fi incarcat."));
  };

  const servedOrders = allOrders.filter((order) => order.status === "SERVITA");

  const isOrderInSelectedPeriod = (order) => {
    if (periodFilter === "ALL") {
      return true;
    }

    const orderDate = new Date(order.createdAt);
    const today = new Date();

    if (periodFilter === "TODAY") {
      return orderDate.toDateString() === today.toDateString();
    }

    if (periodFilter === "LAST_7_DAYS") {
      const sevenDaysAgo = new Date();
      sevenDaysAgo.setDate(today.getDate() - 7);
      return orderDate >= sevenDaysAgo;
    }

    return true;
  };

  const filteredOrders = servedOrders.filter(isOrderInSelectedPeriod);

  const totalSales = filteredOrders.reduce((sum, order) => {
    return sum + Number(order.totalPrice);
  }, 0);

  const productSales = {};

  filteredOrders.forEach((order) => {
    order.items?.forEach((item) => {
      const productName = item.product?.name || "Produs necunoscut";

      if (!productSales[productName]) {
        productSales[productName] = 0;
      }

      productSales[productName] += Number(item.quantity);
    });
  });

  const productSalesList = Object.entries(productSales)
    .map(([name, quantity]) => ({ name, quantity }))
    .sort((a, b) => b.quantity - a.quantity);

  const totalProductsSold = productSalesList.reduce((sum, product) => {
    return sum + product.quantity;
  }, 0);

  const averageRating =
    feedbackList.length === 0
      ? 0
      : feedbackList.reduce((sum, feedback) => {
          return sum + Number(feedback.rating);
        }, 0) / feedbackList.length;

  return (
    <div className="admin-page">
      <header className="admin-header">
        <h1>Panou administrare</h1>
        <p>
          Analiza vanzarilor, produse vandute, feedback clienti si produse
          auxiliare semnalate lipsa.
        </p>

        <button className="logout-button" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      <section className="admin-section">
        <h2>Filtru perioada</h2>

        <select
          value={periodFilter}
          onChange={(e) => setPeriodFilter(e.target.value)}
        >
          <option value="ALL">Toate comenzile</option>
          <option value="TODAY">Azi</option>
          <option value="LAST_7_DAYS">Ultimele 7 zile</option>
        </select>
      </section>

      <section className="admin-section">
        <h2>Indicatori conducere</h2>

        <div className="admin-grid">
          <div className="admin-card">
            <h3>Comenzi finalizate</h3>
            <p>{filteredOrders.length}</p>
          </div>

          <div className="admin-card">
            <h3>Total vanzari</h3>
            <p>{totalSales.toFixed(2)} lei</p>
          </div>

          <div className="admin-card">
            <h3>Produse vandute</h3>
            <p>{totalProductsSold}</p>
          </div>

          <div className="admin-card">
            <h3>Rating mediu</h3>
            <p>{averageRating.toFixed(2)} / 5</p>
          </div>
        </div>
      </section>

      <section className="admin-section">
        <h2>Produse auxiliare lipsa in depozit</h2>

        {unavailableSupplies.length === 0 ? (
          <p>Nu exista produse auxiliare semnalate ca lipsa.</p>
        ) : (
          <div className="admin-grid">
            {unavailableSupplies.map((supply) => (
              <div key={supply.id} className="admin-card">
                <h3>{supply.name}</h3>
                <p>Categorie: {supply.category}</p>
                <p>Status: lipsa in depozit</p>
                <p>
                  Semnalat la:{" "}
                  {supply.reportedAt
                    ? new Date(supply.reportedAt).toLocaleString()
                    : "necunoscut"}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="admin-section">
        <h2>Top produse vandute</h2>

        {productSalesList.length === 0 ? (
          <p>Nu exista produse vandute in perioada selectata.</p>
        ) : (
          <div className="admin-grid">
            {productSalesList.map((product) => (
              <div key={product.name} className="admin-card">
                <h3>{product.name}</h3>
                <p>Cantitate vanduta: {product.quantity}</p>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="admin-section">
        <h2>Recomandare simpla pentru stoc</h2>

        {productSalesList.length === 0 ? (
          <p>Nu exista suficiente date pentru recomandari.</p>
        ) : (
          <div className="admin-grid">
            {productSalesList.slice(0, 3).map((product) => (
              <div key={product.name} className="admin-card">
                <p>
                  Produsul <strong>{product.name}</strong> a fost vandut de{" "}
                  <strong>{product.quantity}</strong> ori. Verifica stocul
                  pentru acest produs.
                </p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default AdminPage;