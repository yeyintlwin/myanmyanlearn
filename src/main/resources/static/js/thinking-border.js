(function () {
  function setLoading(el, loading) {
    if (!el) return;
    el.classList.toggle('thinking-border', !!loading);
  }

  var MML = window.MML || (window.MML = {});
  MML.ui = MML.ui || {};
  MML.ui.thinkingBorder = { setLoading: setLoading };
  window.thinkingBorder = MML.ui.thinkingBorder;
})();
