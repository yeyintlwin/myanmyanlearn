// App runtime: font loading + background application
// (Tailwind CSS is now compiled at build time — see tailwind.config.js)
(function () {
  var defaultFonts = {
    sans: "Ubuntu",
    brand: "Ubuntu",
    mono: "Ubuntu Mono",
    urls: [
      "https://fonts.googleapis.com/css2?family=Ubuntu:wght@300;400;500;700&display=swap",
      "https://fonts.googleapis.com/css2?family=Ubuntu+Mono:wght@400;700&display=swap",
    ],
  };

  var appTheme =
    window.APP_THEME && typeof window.APP_THEME === "object"
      ? window.APP_THEME
      : {};
  var themeFonts =
    appTheme.fonts && typeof appTheme.fonts === "object"
      ? appTheme.fonts
      : defaultFonts;

  // Load Google Fonts
  (function () {
    var urls = Array.isArray(themeFonts.urls)
      ? themeFonts.urls
      : defaultFonts.urls;
    var head = document.head;
    if (!head) return;

    function ensurePreconnect(href, crossOrigin) {
      var selector = 'link[rel="preconnect"][href="' + href + '"]';
      if (head.querySelector(selector)) return;
      var link = document.createElement("link");
      link.rel = "preconnect";
      link.href = href;
      if (crossOrigin) link.crossOrigin = crossOrigin;
      head.appendChild(link);
    }

    function ensureStylesheet(href) {
      var selector = 'link[rel="stylesheet"][href="' + href + '"]';
      if (head.querySelector(selector)) return;
      var link = document.createElement("link");
      link.rel = "stylesheet";
      link.href = href;
      head.appendChild(link);
    }

    ensurePreconnect("https://fonts.googleapis.com");
    ensurePreconnect("https://fonts.gstatic.com", "anonymous");
    urls.forEach(function (u) {
      if (typeof u === "string" && u.trim()) ensureStylesheet(u.trim());
    });
  })();
})();

// App background
(function () {
  var cfg = window.APP_BACKGROUND || {
    mode: "solid",
    color: "#0f172a",
  };
  var root = document.documentElement;
  if (cfg.color) root.style.setProperty("--app-bg-color", cfg.color);
  if (cfg.gradient) root.style.setProperty("--app-bg-gradient", cfg.gradient);

  function apply() {
    var body = document.body;
    if (!body) return;
    body.classList.add("app-bg");
    body.classList.remove("app-bg-solid", "app-bg-gradient");
    if (cfg.mode === "gradient") body.classList.add("app-bg-gradient");
    else body.classList.add("app-bg-solid");
    body.style.backgroundSize = "cover";
    body.style.backgroundPosition = "center";
    body.style.backgroundRepeat = "no-repeat";
    body.style.backgroundAttachment = "fixed";
    if (cfg.mode === "solid") {
      body.style.backgroundImage = "none";
      body.style.backgroundColor = cfg.color || "#000000";
    } else if (cfg.mode === "gradient") {
      var g =
        cfg.gradient ||
        getComputedStyle(document.documentElement).getPropertyValue(
          "--app-bg-gradient",
        ) ||
        "linear-gradient(135deg, #2c001e 0%, #77216f 45%, #e95420 100%)";
      var dim = typeof cfg.dimOpacity === "number" ? cfg.dimOpacity : 0.35;
      var overlay =
        dim > 0
          ? "linear-gradient(rgba(0,0,0," +
            dim +
            "), rgba(0,0,0," +
            dim +
            ")),"
          : "";
      body.style.backgroundImage = overlay + g.trim();
      body.style.backgroundColor = "transparent";
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", apply);
  } else {
    apply();
  }
})();
