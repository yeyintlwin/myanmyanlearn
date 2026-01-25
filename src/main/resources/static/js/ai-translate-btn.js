(function () {
  function setState(btn, opts) {
    if (!btn) return;
    const active = !!(opts && opts.active);
    const loading = !!(opts && opts.loading);
    const pressed =
      opts && typeof opts.pressed === "boolean" ? opts.pressed : active;

    btn.classList.toggle("is-active", active);
    btn.classList.toggle("is-loading", loading);
    btn.setAttribute("aria-pressed", pressed ? "true" : "false");
  }

  window.aiTranslateBtn = {
    setState,
  };
})();
