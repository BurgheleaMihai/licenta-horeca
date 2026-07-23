import UnavailableSuppliesHeader from "../features/admin/unavailable-supplies/components/UnavailableSuppliesHeader";
import UnavailableSuppliesSection from "../features/admin/unavailable-supplies/components/UnavailableSuppliesSection";
import useUnavailableSupplies from "../features/admin/unavailable-supplies/hooks/useUnavailableSupplies";

function AdminUnavailableSuppliesPage() {
  const {
    unavailableSupplies,
    loading,
    errorMessage,
    loadUnavailableSupplies,
  } = useUnavailableSupplies();

  return (
    <div className="admin-page">
      <UnavailableSuppliesHeader
        loading={loading}
        onReload={loadUnavailableSupplies}
      />

      {errorMessage && <p className="error-message">{errorMessage}</p>}

      {loading ? (
        <section className="admin-section">
          <p>Se incarca articolele lipsa...</p>
        </section>
      ) : (
        <UnavailableSuppliesSection unavailableSupplies={unavailableSupplies} />
      )}
    </div>
  );
}

export default AdminUnavailableSuppliesPage;
