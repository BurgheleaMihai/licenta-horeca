/*
 * Explica modul in care sunt filtrate si
 * calculate valorile afisate in statistici.
 */
function StatisticsCalculationNotes() {
  return (
    <section className="admin-statistics-section">
      <h2>Observatii despre calcul</h2>

      <p>
        Comenzile totale si comenzile anulate sunt filtrate dupa momentul
        crearii.
      </p>

      <p>
        Comenzile servite si veniturile sunt filtrate dupa momentul finalizarii
        comenzii.
      </p>

      <p>
        Pentru comenzile vechi care nu au completat campul de finalizare, este
        folosita data crearii.
      </p>
    </section>
  );
}

export default StatisticsCalculationNotes;
