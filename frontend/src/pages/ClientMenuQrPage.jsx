import MenuQrCard from "../features/client/menu/components/MenuQrCard";
import useMenuQr from "../features/client/menu/hooks/useMenuQr";

function ClientMenuQrPage() {
  const { clientMenuUrl, usesLocalhost, handleBackToWaiter } = useMenuQr();

  return (
    <div className="menu-qr-page">
      <MenuQrCard
        clientMenuUrl={clientMenuUrl}
        usesLocalhost={usesLocalhost}
        onBackToWaiter={handleBackToWaiter}
      />
    </div>
  );
}

export default ClientMenuQrPage;
