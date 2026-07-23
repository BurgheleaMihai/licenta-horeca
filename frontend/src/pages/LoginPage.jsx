import LoginForm from "../features/auth/login/components/LoginForm";
import useLogin from "../features/auth/login/hooks/useLogin";

function LoginPage() {
  const {
    email,
    password,
    errorMessage,
    handleEmailChange,
    handlePasswordChange,
    handleLogin,
  } = useLogin();

  return (
    <LoginForm
      email={email}
      password={password}
      errorMessage={errorMessage}
      onEmailChange={handleEmailChange}
      onPasswordChange={handlePasswordChange}
      onSubmit={handleLogin}
    />
  );
}

export default LoginPage;
