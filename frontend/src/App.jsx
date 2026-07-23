import "./App.css";

import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminEmployeesPage from "./pages/AdminEmployeesPage";
import AdminOrderHistoryPage from "./pages/AdminOrderHistoryPage";
import AdminStatisticsPage from "./pages/AdminStatisticsPage";
import AdminStockConfigurationPage from "./pages/AdminStockConfigurationPage";
import AdminUnavailableSuppliesPage from "./pages/AdminUnavailableSuppliesPage";
import BarDashboardPage from "./pages/BarDashboardPage";
import ClientMenuPage from "./pages/ClientMenuPage";
import ClientMenuQrPage from "./pages/ClientMenuQrPage";
import KitchenDashboardPage from "./pages/KitchenDashboardPage";
import LoginPage from "./pages/LoginPage";
import ManagerDashboardPage from "./pages/ManagerDashboardPage";
import ManagerSuppliesPage from "./pages/ManagerSuppliesPage";
import TrafficSensorSimulatorPage from "./pages/TrafficSensorSimulatorPage";
import WaiterDashboardPage from "./pages/WaiterDashboardPage";

function renderProtectedPage(user, requiredRole, PageComponent) {
  if (!user || user.role !== requiredRole) {
    return <LoginPage />;
  }

  return <PageComponent />;
}

function getStoredUser() {
  try {
    const storedUser = localStorage.getItem("user");

    return storedUser ? JSON.parse(storedUser) : null;
  } catch (error) {
    console.error("Utilizatorul salvat nu a putut fi citit:", error);

    localStorage.removeItem("user");
    localStorage.removeItem("token");

    return null;
  }
}

function App() {
  const originalPath = globalThis.location.pathname;

  const path =
    originalPath.length > 1
      ? originalPath.replace(/\/+$/, "").toLowerCase()
      : "/";

  const user = getStoredUser();

  const routes = {
    "/": <ClientMenuPage />,

    "/menu-qr": <ClientMenuQrPage />,

    "/login": <LoginPage />,

    "/waiter": renderProtectedPage(user, "WAITER", WaiterDashboardPage),

    "/kitchen": renderProtectedPage(user, "KITCHEN", KitchenDashboardPage),

    "/bar": renderProtectedPage(user, "BAR", BarDashboardPage),

    "/manager": renderProtectedPage(user, "MANAGER", ManagerDashboardPage),

    "/manager-supplies": renderProtectedPage(
      user,
      "MANAGER",
      ManagerSuppliesPage,
    ),

    "/admin": renderProtectedPage(user, "ADMIN", AdminDashboardPage),

    "/admin/statistics": renderProtectedPage(
      user,
      "ADMIN",
      AdminStatisticsPage,
    ),

    "/admin/order-history": renderProtectedPage(
      user,
      "ADMIN",
      AdminOrderHistoryPage,
    ),

    "/admin/stock-configuration": renderProtectedPage(
      user,
      "ADMIN",
      AdminStockConfigurationPage,
    ),

    "/admin/unavailable-supplies": renderProtectedPage(
      user,
      "ADMIN",
      AdminUnavailableSuppliesPage,
    ),

    "/admin/employees": renderProtectedPage(user, "ADMIN", AdminEmployeesPage),

    "/sensor-simulator": renderProtectedPage(
      user,
      "ADMIN",
      TrafficSensorSimulatorPage,
    ),
  };

  return routes[path] || <LoginPage />;
}

export default App;
