const fs = require("fs");
const path = require("path");

// Die lokalen (Mock-)Translations liegen nicht im Repo, sondern werden nur für die
// lokale Entwicklung unter local_dev_translations/ (neben apps/ und packages/) abgelegt.
// In Production-Builds - und wenn der Ordner gar nicht vorhanden ist - wird der Import
// von services/devTranslations auf services/devTranslations.stub umgebogen, sodass der
// Ordner nicht existieren muss. Siehe packages/core/src/services/i18n.tsx.
const LOCAL_TRANSLATIONS_DIR = path.resolve(__dirname, "local_dev_translations");

// Matcht "./devTranslations" bzw. ".../services/devTranslations", aber nicht
// "./devTranslations.stub" - dadurch kann der Stub selbst normal aufgelöst werden.
const DEV_TRANSLATIONS_MODULE = /(^|\/)devTranslations$/;

const isProduction = process.env.NODE_ENV === "production";
const hasLocalTranslations = fs.existsSync(LOCAL_TRANSLATIONS_DIR);
const useStub = isProduction || !hasLocalTranslations;

if (useStub && !isProduction) {
  console.warn(
    `[metro] ${LOCAL_TRANSLATIONS_DIR} nicht gefunden - devTranslations.stub wird verwendet (keine lokalen Translations).`
  );
}

/**
 * Ergänzt die Metro-Config um das devTranslations -> devTranslations.stub Mapping.
 * Muss nach einem eigenen config.resolver.resolveRequest aufgerufen werden, damit
 * dieses als nächster Resolver in der Kette erhalten bleibt.
 */
function withDevTranslationsStub(config) {
  const previousResolveRequest = config.resolver.resolveRequest;

  config.resolver.resolveRequest = (context, moduleName, platform) => {
    const resolveRequest = previousResolveRequest || context.resolveRequest;

    if (useStub && DEV_TRANSLATIONS_MODULE.test(moduleName)) {
      return resolveRequest(context, `${moduleName}.stub`, platform);
    }

    return resolveRequest(context, moduleName, platform);
  };

  return config;
}

module.exports = { withDevTranslationsStub, useStub };
