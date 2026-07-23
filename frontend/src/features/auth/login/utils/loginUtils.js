import {
  AUTH_STORAGE_KEYS,
  ROLE_REDIRECT_PATHS,
} from "../constants/loginConstants";

/*
 * Salveaza utilizatorul autentificat si tokenul
 * primit de la backend.
 */
export function saveAuthenticatedUser(user) {
  localStorage.setItem(AUTH_STORAGE_KEYS.USER, JSON.stringify(user));

  localStorage.setItem(AUTH_STORAGE_KEYS.TOKEN, user.token);
}

/*
 * Sterge datele de autentificare salvate local.
 */
export function clearAuthenticationStorage() {
  localStorage.removeItem(AUTH_STORAGE_KEYS.USER);

  localStorage.removeItem(AUTH_STORAGE_KEYS.TOKEN);
}

/*
 * Returneaza pagina corespunzatoare rolului
 * utilizatorului autentificat.
 */
export function getRoleRedirectPath(role) {
  return ROLE_REDIRECT_PATHS[role] || null;
}
