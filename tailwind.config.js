/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/main/resources/templates/**/*.html"],
  theme: {
    extend: {
      colors: {
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
      },
      fontFamily: {
        sans: ["Ubuntu", "system-ui", "-apple-system", "Segoe UI", "sans-serif"],
        brand: ["Ubuntu", "system-ui", "-apple-system", "Segoe UI", "sans-serif"],
        mono: ["Ubuntu Mono", "ui-monospace", "monospace"],
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
        glow: "0 0 20px rgba(233, 84, 32, 0.35)",
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
  plugins: [],
};
