function LoginForm({
  email,
  password,
  errorMessage,
  onEmailChange,
  onPasswordChange,
  onSubmit,
}) {
  return (
    <div className="login-container">
      <h1>Autentificare angajat</h1>

      <form onSubmit={onSubmit}>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={onEmailChange}
          required
        />

        <input
          type="password"
          placeholder="Parola"
          value={password}
          onChange={onPasswordChange}
          required
        />

        <button type="submit">Login</button>
      </form>

      {errorMessage && <p>{errorMessage}</p>}
    </div>
  );
}

export default LoginForm;
