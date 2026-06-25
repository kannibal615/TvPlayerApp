(() => {
  const root = document.querySelector("[data-payment-callback]");
  if (!root || !root.dataset.intent) return;

  const title = root.querySelector("[data-payment-title]");
  const message = root.querySelector("[data-payment-message]");
  const accountLink = root.querySelector("[data-payment-account]");
  const setState = (heading, detail) => {
    if (title) title.textContent = heading;
    if (message) message.textContent = detail;
  };

  if (typeof window.GammalVerify !== "function") {
    setState(
      "Vérification indisponible",
      "Le module Gammal Tech n’a pas pu être chargé. Contactez le support si votre banque a confirmé le débit."
    );
    return;
  }

  window.GammalVerify(async (payment) => {
    const status = Number(payment && payment.status);
    if (status !== 1 && status !== 2) {
      setState("Paiement non vérifié", "Gammal Tech n’a pas confirmé cette transaction.");
      return;
    }

    try {
      const response = await fetch(window.location.pathname + window.location.search, {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ intent: root.dataset.intent, payment }),
      });
      const result = await response.json();
      if (!response.ok || !result.success) {
        throw new Error(result.error || "Enregistrement impossible.");
      }
      if (accountLink && result.account_url) accountLink.href = result.account_url;
      setState(
        "Paiement reçu",
        result.duplicate
          ? "Cette transaction avait déjà été enregistrée. Consultez vos commandes."
          : "Votre paiement a été enregistré et attend la validation sécurisée de l’administrateur."
      );
    } catch (error) {
      setState(
        "Paiement à contrôler",
        (error && error.message) ||
          "Le retour n’a pas pu être enregistré. Contactez le support avec votre référence Gammal."
      );
    }
  });
})();
