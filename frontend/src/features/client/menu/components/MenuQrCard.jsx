import { QRCodeSVG } from "qrcode.react";

function MenuQrCard({ clientMenuUrl, usesLocalhost, onBackToWaiter }) {
  return (
    <section className="menu-qr-card">
      <h1>Meniu digital</h1>

      <p className="menu-qr-description">
        Scaneaza codul QR pentru a consulta meniul restaurantului.
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
          Adresa foloseste localhost. Pentru scanarea de pe telefon,
          configureaza VITE_CLIENT_BASE_URL cu adresa IP a calculatorului.
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
          onClick={onBackToWaiter}
        >
          Inapoi la ospatar
        </button>
      </div>
    </section>
  );
}

export default MenuQrCard;
