import AdminOrderHistoryPage from "./pages/AdminOrderHistoryPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminStatisticsPage from "./pages/AdminStatisticsPage";
import AdminStockConfigurationPage from "./pages/AdminStockConfigurationPage";
import AdminUnavailableSuppliesPage from "./pages/AdminUnavailableSuppliesPage";
import BarDashboardPage from "./pages/BarDashboardPage";
import ClientMenuPage from "./pages/ClientMenuPage";
import AdminEmployeesPage from "./pages/AdminEmployeesPage";
import KitchenDashboardPage from "./pages/KitchenDashboardPage";
import LoginPage from "./pages/LoginPage";
import ManagerDashboardPage from "./pages/ManagerDashboardPage";
import ManagerSuppliesPage from "./pages/ManagerSuppliesPage";
import ClientMenuQrPage from "./pages/ClientMenuQrPage";
import SensorSimulatorPage from "./pages/SensorSimulatorPage";
import WaiterDashboardPage from "./pages/WaiterDashboardPage";
import "./App.css";

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

    "/sensor-simulator": <SensorSimulatorPage />,
  };

  return routes[path] || <LoginPage />;
}

export default App;
