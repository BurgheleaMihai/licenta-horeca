import { useState } from "react";
import { login } from "../api/authApi";

function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const handleLogin = async (event) => {
    event.preventDefault();
    setErrorMessage("");

    try {
      const response = await login({
        email,
        password,
      });

      const user = response.data;

      if (!user.token) {
        setErrorMessage("Tokenul de autentificare nu a fost primit.");
        return;
      }

      localStorage.setItem("user", JSON.stringify(user));
      localStorage.setItem("token", user.token);

      if (user.role === "WAITER") {
        globalThis.location.href = "/waiter";
      } else if (user.role === "KITCHEN") {
        globalThis.location.href = "/kitchen";
      } else if (user.role === "BAR") {
        globalThis.location.href = "/bar";
      } else if (user.role === "MANAGER") {
        globalThis.location.href = "/manager";
      } else if (user.role === "ADMIN") {
        globalThis.location.href = "/admin";
      } else {
        localStorage.removeItem("user");
        localStorage.removeItem("token");

        setErrorMessage("Rol necunoscut.");
      }
    } catch {
      localStorage.removeItem("user");
      localStorage.removeItem("token");

      setErrorMessage("Email sau parola incorecta.");
    }
  };

  return (
    <div className="login-container">
      <h1>Autentificare angajat</h1>

      <form onSubmit={handleLogin}>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />

        <input
          type="password"
          placeholder="Parola"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />

        <button type="submit">Login</button>
      </form>

      {errorMessage && <p>{errorMessage}</p>}
    </div>
  );
}

export default LoginPage;