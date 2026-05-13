import ClientMenuPage from "./pages/ClientMenuPage";
import WaiterPage from "./pages/WaiterPage";
import KitchenPage from "./pages/KitchenPage";
import BarPage from "./pages/BarPage";
import LoginPage from "./pages/LoginPage";
import "./App.css";
import ManagerPage from "./pages/ManagerPage";
import AdminPage from "./pages/AdminPage";
import SensorSimulatorPage from "./pages/SensorSimulatorPage";
import ManagerSuppliesPage from "./pages/ManagerSuppliesPage";

function renderProtectedPage(user, requiredRole, PageComponent) {
  if (!user || user.role !== requiredRole) {
    return <LoginPage />;
  }

  return <PageComponent />;
}

function App() {
  const path = globalThis.location.pathname;
  const user = JSON.parse(localStorage.getItem("user"));

  const routes = {
    "/login": <LoginPage />,
    "/waiter": renderProtectedPage(user, "WAITER", WaiterPage),
    "/kitchen": renderProtectedPage(user, "KITCHEN", KitchenPage),
    "/bar": renderProtectedPage(user, "BAR", BarPage),
    "/manager": renderProtectedPage(user, "MANAGER", ManagerPage),
    "/admin": renderProtectedPage(user, "ADMIN", AdminPage),
    "/sensor-simulator": <SensorSimulatorPage />,
    "/manager-supplies": renderProtectedPage(user, "MANAGER", ManagerSuppliesPage)
  };

  return routes[path] || <ClientMenuPage />;
}

export default App;