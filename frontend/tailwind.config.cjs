/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ["Fraunces", "serif"],
        body: ["Space Grotesk", "sans-serif"]
      },
      colors: {
        ink: "#111827",
        mint: "#0f766e",
        ember: "#ea580c",
        haze: "#ecfeff"
      }
    }
  },
  plugins: []
};

