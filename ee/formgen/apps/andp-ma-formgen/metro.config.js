const { getDefaultConfig } = require("expo/metro-config");
const fs = require("fs");
const path = require("path");
const { resolve } = require("metro-resolver");

const projectRoot = __dirname;
const monorepoRoot = path.resolve(projectRoot, "../..");

const isProduction = process.env.NODE_ENV === "production";
const translationsPath = path.resolve(monorepoRoot, "./translations");
const hasTranslations = fs.existsSync(translationsPath);

const config = getDefaultConfig(projectRoot);

config.watchFolders = [monorepoRoot];
if (hasTranslations) {
  config.watchFolders.push(translationsPath);
}

config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, "node_modules"),
  path.resolve(monorepoRoot, "node_modules"),
];

const { transformer, resolver } = config;

config.transformer = {
  ...transformer,
  babelTransformerPath: require.resolve("react-native-svg-transformer/expo")
};

config.resolver = {
  ...resolver,
  platforms: ["web"],
  assetExts: resolver.assetExts.filter((ext) => ext !== "svg"),
  sourceExts: [...resolver.sourceExts, "svg"]
};

// 🧩 REACT-PDF UND PDFJS-DIST DEAKTIVIEREN
config.resolver.extraNodeModules = {
  ...config.resolver.extraNodeModules
};


config.resolver.disableHierarchicalLookup = true;

// In Production-Builds (oder wenn der translations/-Ordner fehlt) wird das
// devTranslations-Modul auf einen leeren Stub umgemappt, damit die statischen
// JSON-Imports nicht aufgelöst werden müssen.
const devTranslationsStub = path.resolve(
  monorepoRoot,
  "packages/core/src/services/devTranslations.stub.ts"
);
const shouldStubTranslations = isProduction || !hasTranslations;

config.resolver.resolveRequest = (context, moduleName, platform) => {
  if (
    shouldStubTranslations &&
    (moduleName === "./devTranslations" || moduleName.endsWith("/devTranslations"))
  ) {
    return { type: "sourceFile", filePath: devTranslationsStub };
  }
  if (moduleName === "pdfjs-dist/build/pdf.mjs") {
    return resolve(context, "pdfjs-dist/legacy/build/pdf.js", platform);
  }
  if (moduleName === "pdfjs-dist/build/pdf.worker.mjs") {
    return resolve(context, "pdfjs-dist/legacy/build/pdf.worker.js", platform);
  }
  return resolve(context, moduleName, platform);
};

module.exports = config;