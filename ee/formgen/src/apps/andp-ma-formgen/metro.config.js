const { getDefaultConfig } = require("expo/metro-config");
const path = require("path");
const { resolve } = require("metro-resolver");

const projectRoot = __dirname;
const monorepoRoot = path.resolve(projectRoot, "../..");

const config = getDefaultConfig(projectRoot);

config.watchFolders = [
  monorepoRoot,
  path.resolve(monorepoRoot, "../cms/packages-common/andp/files/translations")
];

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

config.resolver.resolveRequest = (context, moduleName, platform) => {
  if (moduleName === "pdfjs-dist/build/pdf.mjs") {
    return resolve(context, "pdfjs-dist/legacy/build/pdf.js", platform);
  }
  if (moduleName === "pdfjs-dist/build/pdf.worker.mjs") {
    return resolve(context, "pdfjs-dist/legacy/build/pdf.worker.js", platform);
  }
  return resolve(context, moduleName, platform);
};

module.exports = config;