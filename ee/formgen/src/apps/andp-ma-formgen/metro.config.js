const { getDefaultConfig } = require("expo/metro-config");
const path = require("path");
const { withDevTranslationsStub } = require("../../metro.dev-translations");

const projectRoot = __dirname;
const monorepoRoot = path.resolve(projectRoot, "../..");

const config = getDefaultConfig(projectRoot);

// 1. Watch-Folders für das Monorepo (Packages & Root node_modules)
config.watchFolders = [monorepoRoot];

// 2. Hierarchischer Lookup wird in Expo 56 ZWINGEND benötigt!
// Wir entfernen nodeModulesPaths und disableHierarchicalLookup komplett, 
// da Metro den Baum jetzt nativ von der App bis zum Root scannt.

// 3. SVG Transformer Setup
const { transformer, resolver } = config;

config.transformer = {
  ...transformer,
  babelTransformerPath: require.resolve("react-native-svg-transformer/expo")
};

config.resolver = {
  ...resolver,
  // platforms NICHT auf "web" hardcoden! Metro reicht die Plattform dynamisch rein.
  assetExts: resolver.assetExts.filter((ext) => ext !== "svg"),
  sourceExts: [...resolver.sourceExts, "svg"]
};

// 4. PDFJS-DIST Legacy-Umleitungen beibehalten
config.resolver.resolveRequest = (context, moduleName, platform) => {
  if (moduleName === "pdfjs-dist/build/pdf.mjs") {
    // Nutze den context.redirect- oder Standard-Resolver von Metro ohne harte Überschreibung
    return context.resolveRequest(context, "pdfjs-dist/legacy/build/pdf.js", platform);
  }
  if (moduleName === "pdfjs-dist/build/pdf.worker.mjs") {
    return context.resolveRequest(context, "pdfjs-dist/legacy/build/pdf.worker.js", platform);
  }
  // Standard-Resolver aufrufen
  return context.resolveRequest(context, moduleName, platform);
};

module.exports = withDevTranslationsStub(config);
