import ClientMenuPage from "./pages/ClientMenuPage";
import WaiterPage from "./pages/WaiterPage";
import "./App.css";
import KitchenPage from "./pages/KitchenPage";
import BarPage from "./pages/BarPage";

function App() {
  const path = window.location.pathname;

  if (path === "/waiter") {
    return <WaiterPage />;
  }

  if (path === "/kitchen") {
    return <KitchenPage />;
  }

  if (path === "/bar") {
    return <BarPage />;
  }

  return <ClientMenuPage />;
}

export default App;