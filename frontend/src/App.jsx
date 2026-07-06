import ClientMenuPage from "./pages/ClientMenuPage";
import WaiterPage from "./pages/WaiterPage";
import KitchenPage from "./pages/KitchenPage";
import BarPage from "./pages/BarPage";
import LoginPage from "./pages/LoginPage";
import ManagerPage from "./pages/ManagerPage";
import AdminPage from "./pages/AdminPage";
import SensorSimulatorPage from "./pages/SensorSimulatorPage";
import ManagerSuppliesPage from "./pages/ManagerSuppliesPage";
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
    "/login": <LoginPage />,

    "/waiter": renderProtectedPage(
      user,
      "WAITER",
      WaiterPage
    ),

    "/kitchen": renderProtectedPage(
      user,
      "KITCHEN",
      KitchenPage
    ),

    "/bar": renderProtectedPage(
      user,
      "BAR",
      BarPage
    ),

    "/manager": renderProtectedPage(
      user,
      "MANAGER",
      ManagerPage
    ),

    "/admin": renderProtectedPage(
      user,
      "ADMIN",
      AdminPage
    ),

    "/sensor-simulator": <SensorSimulatorPage />,

    "/manager-supplies": renderProtectedPage(
      user,
      "MANAGER",
      ManagerSuppliesPage
    ),
  };

  return routes[path] || <LoginPage />;
}

export default App;