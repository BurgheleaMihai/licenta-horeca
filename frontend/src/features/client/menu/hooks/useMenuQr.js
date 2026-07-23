import { getClientMenuUrl, isLocalhostUrl } from "../utils/menuQrUtils";

function useMenuQr() {
  const clientMenuUrl = getClientMenuUrl();

  const usesLocalhost = isLocalhostUrl(clientMenuUrl);

  const handleBackToWaiter = () => {
    globalThis.location.href = "/waiter";
  };

  return {
    clientMenuUrl,
    usesLocalhost,
    handleBackToWaiter,
  };
}

export default useMenuQr;
