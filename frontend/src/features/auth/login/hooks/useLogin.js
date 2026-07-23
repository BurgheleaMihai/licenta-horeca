import { useState } from "react";

import { login } from "../../../../api/authApi";
import {
  clearAuthenticationStorage,
  getRoleRedirectPath,
  saveAuthenticatedUser,
} from "../utils/loginUtils";

function useLogin() {
  const [email, setEmail] = useState("");

  const [password, setPassword] = useState("");

  const [errorMessage, setErrorMessage] = useState("");

  const handleEmailChange = (event) => {
    setEmail(event.target.value);
  };

  const handlePasswordChange = (event) => {
    setPassword(event.target.value);
  };

  const handleLogin = async (event) => {
    event.preventDefault();

    setErrorMessage("");

    try {
      const response = await login({
        email,
        password,
      });

      const user = response.data;

      if (!user?.token) {
        clearAuthenticationStorage();

        setErrorMessage("Tokenul de autentificare nu a fost primit.");

        return;
      }

      const redirectPath = getRoleRedirectPath(user.role);

      if (!redirectPath) {
        clearAuthenticationStorage();

        setErrorMessage("Rol necunoscut.");

        return;
      }

      saveAuthenticatedUser(user);

      globalThis.location.href = redirectPath;
    } catch (error) {
      console.error("Eroare la autentificare:", error);

      clearAuthenticationStorage();

      setErrorMessage("Email sau parola incorecta.");
    }
  };

  return {
    email,
    password,
    errorMessage,
    handleEmailChange,
    handlePasswordChange,
    handleLogin,
  };
}

export default useLogin;
