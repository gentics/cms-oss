export default {
  expo: {
    name: "andp-ma-formgen",
    slug: "andp-ma-formgen",
    version: "0.0.1",
    orientation: "portrait",
    icon: "./assets/icon.png",
    userInterfaceStyle: "light",
    splash: {
      image: "./assets/splash.png",
      resizeMode: "contain",
      backgroundColor: "#ffffff",
    },
    web: {
      favicon: "./assets/favicon.png",
    },
    experiments: {
      tsconfigPaths: true,
      baseUrl: process.env.APP_BASE_URL || '/',
    },
    extra: {
      devMode: process.env.DEV_MODE === "true",
      mockForm: process.env.MOCK_FORM === "true",
    },
  },
};