import { QRCodeSVG } from "qrcode.react";

const getClientMenuUrl = () => {
  const configuredUrl =
    import.meta.env.VITE_CLIENT_BASE_URL
      ?.trim();

  const baseUrl =
    configuredUrl
    || globalThis.location.origin;

  return `${baseUrl.replace(/\/+$/, "")}/`;
};

function MenuQrPage() {
  const clientMenuUrl =
    getClientMenuUrl();

  const usesLocalhost =
    /\/\/(localhost|127\.0\.0\.1)(:\d+)?/i
      .test(clientMenuUrl);

  const handleBackToWaiter = () => {
    globalThis.location.href = "/waiter";
  };

  return (
    <div className="menu-qr-page">
      <section className="menu-qr-card">
        <h1>Meniu digital</h1>

        <p className="menu-qr-description">
          Scaneaza codul QR pentru a consulta
          meniul restaurantului.
        </p>

        <div className="menu-qr-code">
          <QRCodeSVG
            value={clientMenuUrl}
            size={280}
            level="M"
            marginSize={4}
            title="Cod QR pentru meniul clientului"
          />
        </div>

        {usesLocalhost && (
          <p className="menu-qr-warning">
            Adresa foloseste localhost. Pentru
            scanarea de pe telefon, configureaza
            VITE_CLIENT_BASE_URL cu adresa IP a
            calculatorului.
          </p>
        )}

        <div className="menu-qr-actions">
          <a
            className="menu-qr-link"
            href={clientMenuUrl}
            target="_blank"
            rel="noreferrer"
          >
            Deschide meniul
          </a>

          <button
            type="button"
            className="menu-qr-back-button"
            onClick={handleBackToWaiter}
          >
            Inapoi la ospatar
          </button>
        </div>
      </section>
    </div>
  );
}

export default MenuQrPage;