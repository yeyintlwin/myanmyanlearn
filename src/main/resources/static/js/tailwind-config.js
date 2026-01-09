// Tailwind CSS Configuration for Myan Myan Learn
(function () {
  var defaultColors = {
    primary: {
      50: "#fff4ef",
      100: "#ffe5d9",
      200: "#ffc7ad",
      300: "#ffa074",
      400: "#f9783b",
      500: "#e95420",
      600: "#d44418",
      700: "#b13613",
      800: "#8a2a0f",
      900: "#65200b",
    },
    secondary: {
      50: "#f6eef6",
      100: "#ead6e9",
      200: "#d3add1",
      300: "#b97db6",
      400: "#9d5297",
      500: "#77216f",
      600: "#641b5d",
      700: "#50154a",
      800: "#3c0f37",
      900: "#2c001e",
    },
    accent: {
      50: "#fff7ed",
      100: "#ffedd5",
      200: "#fed7aa",
      300: "#fdba74",
      400: "#fb923c",
      500: "#f67400",
      600: "#e85d00",
      700: "#c24600",
      800: "#9a3600",
      900: "#7c2d12",
    },
  };

  var appTheme =
    window.APP_THEME && typeof window.APP_THEME === "object"
      ? window.APP_THEME
      : {};
  var themeColors =
    appTheme.colors && typeof appTheme.colors === "object"
      ? appTheme.colors
      : defaultColors;

  var defaultFonts = {
    sans: "Ubuntu",
    brand: "Ubuntu",
    mono: "Ubuntu Mono",
    urls: [
      "https://fonts.googleapis.com/css2?family=Ubuntu:wght@300;400;500;700&display=swap",
      "https://fonts.googleapis.com/css2?family=Ubuntu+Mono:wght@400;700&display=swap",
    ],
  };

  var themeFonts =
    appTheme.fonts && typeof appTheme.fonts === "object"
      ? appTheme.fonts
      : defaultFonts;

  var fontSans =
    typeof themeFonts.sans === "string" && themeFonts.sans.trim()
      ? themeFonts.sans.trim()
      : defaultFonts.sans;
  var fontBrand =
    typeof themeFonts.brand === "string" && themeFonts.brand.trim()
      ? themeFonts.brand.trim()
      : defaultFonts.brand;
  var fontMono =
    typeof themeFonts.mono === "string" && themeFonts.mono.trim()
      ? themeFonts.mono.trim()
      : defaultFonts.mono;

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

    var root = document.documentElement;
    if (!root) return;
    root.style.setProperty("--app-font-sans", fontSans);
    root.style.setProperty("--app-font-brand", fontBrand);
    root.style.setProperty("--app-font-mono", fontMono);
  })();

  function hexToRgbTriplet(hex) {
    if (!hex || typeof hex !== "string") return null;
    var h = hex.trim().replace("#", "");
    if (h.length !== 6) return null;
    var r = parseInt(h.slice(0, 2), 16);
    var g = parseInt(h.slice(2, 4), 16);
    var b = parseInt(h.slice(4, 6), 16);
    if (
      [r, g, b].some(function (n) {
        return Number.isNaN(n);
      })
    )
      return null;
    return r + ", " + g + ", " + b;
  }

  var glowRgb =
    hexToRgbTriplet(themeColors.primary && themeColors.primary[500]) ||
    "233, 84, 32";

  var defaultBgGradient =
    "linear-gradient(135deg, " +
    ((themeColors.secondary && themeColors.secondary[900]) || "#2c001e") +
    " 0%, " +
    ((themeColors.secondary && themeColors.secondary[500]) || "#77216f") +
    " 45%, " +
    ((themeColors.primary && themeColors.primary[500]) || "#e95420") +
    " 100%)";

  tailwind.config = {
    theme: {
      extend: {
        colors: themeColors,
        fontFamily: {
          sans: [
            "var(--app-font-sans)",
            "system-ui",
            "-apple-system",
            "Segoe UI",
            "sans-serif",
          ],
          brand: [
            "var(--app-font-brand)",
            "system-ui",
            "-apple-system",
            "Segoe UI",
            "sans-serif",
          ],
          mono: ["var(--app-font-mono)", "ui-monospace", "monospace"],
        },
        fontSize: {
          xs: ["0.75rem", { lineHeight: "1rem" }],
          sm: ["0.875rem", { lineHeight: "1.25rem" }],
          base: ["1rem", { lineHeight: "1.5rem" }],
          lg: ["1.125rem", { lineHeight: "1.75rem" }],
          xl: ["1.25rem", { lineHeight: "1.75rem" }],
          "2xl": ["1.5rem", { lineHeight: "2rem" }],
          "3xl": ["1.875rem", { lineHeight: "2.25rem" }],
          "4xl": ["2.25rem", { lineHeight: "2.5rem" }],
          "5xl": ["3rem", { lineHeight: "1" }],
          "6xl": ["3.75rem", { lineHeight: "1" }],
        },
        spacing: {
          18: "4.5rem",
          88: "22rem",
          128: "32rem",
        },
        borderRadius: {
          xl: "0.75rem",
          "2xl": "1rem",
          "3xl": "1.5rem",
        },
        boxShadow: {
          glass: "0 8px 32px 0 rgba(44, 0, 30, 0.35)",
          glow: "0 0 20px rgba(" + glowRgb + ", 0.35)",
          soft: "0 2px 15px -3px rgba(0, 0, 0, 0.07), 0 10px 20px -2px rgba(0, 0, 0, 0.04)",
        },
        backdropBlur: {
          xs: "2px",
        },
        animation: {
          "fade-in": "fadeIn 0.5s ease-in-out",
          "slide-up": "slideUp 0.3s ease-out",
          "bounce-soft": "bounceSoft 0.6s ease-in-out",
          "pulse-soft": "pulseSoft 2s cubic-bezier(0.4, 0, 0.6, 1) infinite",
        },
        keyframes: {
          fadeIn: {
            "0%": { opacity: "0" },
            "100%": { opacity: "1" },
          },
          slideUp: {
            "0%": { transform: "translateY(10px)", opacity: "0" },
            "100%": { transform: "translateY(0)", opacity: "1" },
          },
          bounceSoft: {
            "0%, 100%": { transform: "translateY(0)" },
            "50%": { transform: "translateY(-5px)" },
          },
          pulseSoft: {
            "0%, 100%": { opacity: "1" },
            "50%": { opacity: "0.8" },
          },
        },
      },
    },
    plugins: [
      function (_ref) {
        var addUtilities = _ref.addUtilities;
        var newUtilities = {
          ".glass": {
            background: "rgba(255, 255, 255, 0.1)",
            "backdrop-filter": "blur(10px)",
            border: "1px solid rgba(255, 255, 255, 0.2)",
          },
          ".glass-dark": {
            background: "rgba(0, 0, 0, 0.1)",
            "backdrop-filter": "blur(10px)",
            border: "1px solid rgba(255, 255, 255, 0.1)",
          },
          ".gradient-primary": {
            background:
              "linear-gradient(135deg, " +
              ((themeColors.primary && themeColors.primary[500]) || "#e95420") +
              " 0%, " +
              ((themeColors.secondary && themeColors.secondary[500]) ||
                "#77216f") +
              " 100%)",
          },
          ".gradient-primary-old": {
            background:
              "linear-gradient(135deg, " +
              ((themeColors.primary && themeColors.primary[500]) || "#e95420") +
              " 0%, " +
              ((themeColors.secondary && themeColors.secondary[500]) ||
                "#77216f") +
              " 100%)",
          },
          ".app-bg": {
            "background-size": "cover",
            "background-position": "center",
            "background-repeat": "no-repeat",
            "background-attachment": "fixed",
          },
          ".app-bg-solid": {
            "background-color": "var(--app-bg-color, #000000)",
          },
          ".app-bg-gradient": {
            "background-image":
              "var(--app-bg-gradient, " + defaultBgGradient + ")",
          },
          ".gradient-accent": {
            background:
              "linear-gradient(135deg, " +
              ((themeColors.primary && themeColors.primary[500]) || "#e95420") +
              " 0%, " +
              ((themeColors.accent && themeColors.accent[500]) || "#f67400") +
              " 100%)",
          },
          ".text-gradient": {
            background:
              "linear-gradient(135deg, " +
              ((themeColors.primary && themeColors.primary[500]) || "#e95420") +
              " 0%, " +
              ((themeColors.secondary && themeColors.secondary[500]) ||
                "#77216f") +
              " 100%)",
            "-webkit-background-clip": "text",
            "-webkit-text-fill-color": "transparent",
            "background-clip": "text",
          },
          ".brand-font": {
            "font-family":
              "var(--app-font-brand), system-ui, -apple-system, Segoe UI, sans-serif",
            "font-weight": "700",
          },
        };
        addUtilities(newUtilities);
      },
    ],
  };
})();

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
    // Force inline style to override any existing Tailwind bg-* classes
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
          "--app-bg-gradient"
        ) ||
        "linear-gradient(135deg, #2c001e 0%, #77216f 45%, #e95420 100%)";
      var dim = typeof cfg.dimOpacity === "number" ? cfg.dimOpacity : 0.35;
      var overlay =
        dim > 0
          ? "linear-gradient(rgba(0,0,0," + dim + "), rgba(0,0,0," + dim + ")),"
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
