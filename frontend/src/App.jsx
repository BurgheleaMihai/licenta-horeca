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

function App() {
  const path = window.location.pathname;

  const user = JSON.parse(localStorage.getItem("user"));

  if (path === "/login") {
    return <LoginPage />;
  }

  if (path === "/waiter") {
    if (!user || user.role !== "WAITER") {
      return <LoginPage />;
    }
    return <WaiterPage />;
  }

  if (path === "/kitchen") {
    if (!user || user.role !== "KITCHEN") {
      return <LoginPage />;
    }
    return <KitchenPage />;
  }

  if (path === "/bar") {
    if (!user || user.role !== "BAR") {
      return <LoginPage />;
    }
    return <BarPage />;
  }

  if (path === "/manager") {
    if (!user || user.role !== "MANAGER") {
      return <LoginPage />;
    }
    return <ManagerPage />;
  }

  if (path === "/admin") {
    if (!user || user.role !== "ADMIN") {
      return <LoginPage />;
    }
    return <AdminPage />;
  }

  if (path === "/sensor-simulator") {
    return <SensorSimulatorPage />;
  }

  if (path === "/manager-supplies") {
    if (!user || user.role !== "MANAGER") {
      return <LoginPage />;
    }
    return <ManagerSuppliesPage />;
  }

  return <ClientMenuPage />;
}

export default App;