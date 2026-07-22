import AdminOrderHistoryPage from "./pages/AdminOrderHistoryPage";
import AdminPage from "./pages/AdminPage";
import AdminStatisticsPage from "./pages/AdminStatisticsPage";
import AdminStockConfigurationPage from "./pages/AdminStockConfigurationPage";
import BarPage from "./pages/BarPage";
import ClientMenuPage from "./pages/ClientMenuPage";
import EmployeeManagementPage from "./pages/EmployeeManagementPage";
import KitchenPage from "./pages/KitchenPage";
import LoginPage from "./pages/LoginPage";
import ManagerPage from "./pages/ManagerPage";
import ManagerSuppliesPage from "./pages/ManagerSuppliesPage";
import MenuQrPage from "./pages/MenuQrPage";
import SensorSimulatorPage from "./pages/SensorSimulatorPage";
import WaiterPage from "./pages/WaiterPage";
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

    "/menu-qr": <MenuQrPage />,

    "/login": <LoginPage />,

    "/waiter": renderProtectedPage(user, "WAITER", WaiterPage),

    "/kitchen": renderProtectedPage(user, "KITCHEN", KitchenPage),

    "/bar": renderProtectedPage(user, "BAR", BarPage),

    "/manager": renderProtectedPage(user, "MANAGER", ManagerPage),

    "/manager-supplies": renderProtectedPage(
      user,
      "MANAGER",
      ManagerSuppliesPage,
    ),

    "/admin": renderProtectedPage(user, "ADMIN", AdminPage),

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

    "/admin/employees": renderProtectedPage(
      user,
      "ADMIN",
      EmployeeManagementPage,
    ),

    "/sensor-simulator": <SensorSimulatorPage />,
  };

  return routes[path] || <LoginPage />;
}

export default App;
