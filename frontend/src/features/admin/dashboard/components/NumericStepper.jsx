import { useEffect, useRef } from "react";

function NumericStepper({ id, label, value, onChange }) {
  /*
   * Valoarea primita este pastrata ca text in pagina parinte,
   * deoarece este afisata intr-un input controlat.
   *
   * Pentru operatiile de incrementare si decrementare,
   * valoarea este transformata temporar in numar.
   */
  const numericValue = Number(value) || 0;

  /*
   * Retine valoarea numerica actuala.
   *
   * Este folosit un ref deoarece functiile pornite de
   * setTimeout si setInterval trebuie sa aiba permanent
   * acces la cea mai recenta valoare.
   */
  const currentValueRef = useRef(numericValue);

  /*
   * Retine temporizatorul care asteapta pana cand
   * apasarea este considerata o apasare lunga.
   */
  const holdTimeoutRef = useRef(null);

  /*
   * Retine intervalul care modifica repetat valoarea
   * cat timp utilizatorul tine butonul apasat.
   */
  const holdIntervalRef = useRef(null);

  useEffect(() => {
    currentValueRef.current = numericValue;
  }, [numericValue]);

  /*
   * Opreste temporizatoarele folosite pentru
   * incrementarea sau decrementarea continua.
   */
  const stopHolding = () => {
    if (holdTimeoutRef.current !== null) {
      clearTimeout(holdTimeoutRef.current);
      holdTimeoutRef.current = null;
    }

    if (holdIntervalRef.current !== null) {
      clearInterval(holdIntervalRef.current);
      holdIntervalRef.current = null;
    }
  };

  /*
   * Curata temporizatoarele atunci cand componenta
   * este eliminata din pagina.
   */
  useEffect(() => {
    return stopHolding;
  }, []);

  /*
   * Creste sau scade valoarea curenta.
   *
   * Math.max impiedica obtinerea unei valori
   * mai mici decat zero.
   */
  const changeValueBy = (difference) => {
    const nextValue = Math.max(0, currentValueRef.current + difference);

    currentValueRef.current = nextValue;

    /*
     * Trimite noua valoare catre pagina parinte.
     */
    onChange(String(nextValue));

    /*
     * Daca decrementarea a ajuns la zero,
     * repetarea este oprita automat.
     */
    if (difference < 0 && nextValue === 0) {
      stopHolding();
    }
  };

  /*
   * Porneste modificarea valorii atunci cand
   * utilizatorul apasa butonul minus sau plus.
   */
  const startHolding = (event, difference) => {
    event.preventDefault();

    stopHolding();

    /*
     * Prima modificare este executata imediat.
     */
    changeValueBy(difference);

    try {
      /*
       * Pastreaza controlul pointerului pe buton
       * pe durata apasarii.
       */
      event.currentTarget.setPointerCapture(event.pointerId);
    } catch {
      /*
       * Capturarea pointerului nu este obligatorie.
       */
    }

    /*
     * Dupa 400 ms incepe modificarea continua.
     */
    holdTimeoutRef.current = setTimeout(() => {
      /*
       * Valoarea este modificata la fiecare 90 ms.
       */
      holdIntervalRef.current = setInterval(() => {
        changeValueBy(difference);
      }, 90);
    }, 400);
  };

  /*
   * Permite folosirea butoanelor si prin tastatura.
   *
   * event.detail === 0 indica de regula un click
   * produs prin tastatura.
   */
  const handleKeyboardClick = (event, difference) => {
    if (event.detail === 0) {
      changeValueBy(difference);
    }
  };

  /*
   * Permite introducerea manuala doar a cifrelor.
   *
   * Valoarea goala este permisa temporar pentru
   * ca utilizatorul sa poata sterge continutul.
   */
  const handleInputChange = (event) => {
    const nextValue = event.target.value;

    if (nextValue === "" || /^\d+$/.test(nextValue)) {
      onChange(nextValue);
    }
  };

  /*
   * Daca utilizatorul paraseste inputul gol,
   * valoarea este resetata la zero.
   */
  const handleInputBlur = () => {
    if (value === "") {
      onChange("0");
    }
  };

  return (
    <div className="filter-group compact-number-field">
      <label htmlFor={id}>{label}</label>

      <div className="number-stepper">
        <button
          type="button"
          className="number-stepper-button"
          onPointerDown={(event) => startHolding(event, -1)}
          onPointerUp={stopHolding}
          onPointerCancel={stopHolding}
          onPointerLeave={stopHolding}
          onClick={(event) => handleKeyboardClick(event, -1)}
          disabled={numericValue <= 0}
          aria-label={`Scade ${label}`}
          title="Apasa sau tine apasat"
        >
          −
        </button>

        <input
          id={id}
          type="number"
          min="0"
          step="1"
          inputMode="numeric"
          value={value}
          onChange={handleInputChange}
          onBlur={handleInputBlur}
        />

        <button
          type="button"
          className="number-stepper-button"
          onPointerDown={(event) => startHolding(event, 1)}
          onPointerUp={stopHolding}
          onPointerCancel={stopHolding}
          onPointerLeave={stopHolding}
          onClick={(event) => handleKeyboardClick(event, 1)}
          aria-label={`Creste ${label}`}
          title="Apasa sau tine apasat"
        >
          +
        </button>
      </div>
    </div>
  );
}

export default NumericStepper;
