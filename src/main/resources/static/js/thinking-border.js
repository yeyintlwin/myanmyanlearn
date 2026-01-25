(function () {
  function setLoading(el, loading) {
    if (!el) return;
    el.classList.toggle('thinking-border', !!loading);
  }

  window.thinkingBorder = {
    setLoading,
  };
})();
