/*
 * Construieste adresa publica a meniului.
 * Variabila VITE_CLIENT_BASE_URL are prioritate
 * fata de adresa curenta a aplicatiei.
 */
export function getClientMenuUrl() {
  const configuredUrl = import.meta.env.VITE_CLIENT_BASE_URL?.trim();

  const baseUrl = configuredUrl || globalThis.location.origin;

  return `${baseUrl.replace(/\/+$/, "")}/`;
}

/*
 * Verifica daca adresa QR foloseste o adresa
 * locala care nu poate fi accesata direct
 * de pe un alt dispozitiv.
 */
export function isLocalhostUrl(url) {
  return /\/\/(localhost|127\.0\.0\.1)(:\d+)?/i.test(url);
}
