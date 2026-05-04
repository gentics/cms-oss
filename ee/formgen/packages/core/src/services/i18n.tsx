import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import Backend from 'i18next-http-backend';
import Constants from 'expo-constants';

// Lokale (Mock-)Translations – werden in Production-Builds via metro.config.js
// auf devTranslations.stub.ts gemappt, sodass der translations/-Ordner dann
// nicht existieren muss.
import { mergedDe, mergedEn } from './devTranslations';
import { Platform } from 'react-native';
/**
 * Initialisiert i18n.
 * @param translationUrl Wenn vorhanden => wird im Backend-LoadPath verwendet
 * @param isDev Wenn true => Lokale Datei statt http-Backend
 * @param culture Der zu verwendende Sprachcode (z.B. 'en' oder 'de')
 */
export async function initI18n(
  meshEndpoint: string,
  translationUrl?: string,
  isDev = false,
  culture: string = 'de', // Standard: 'de'
) {
  if (i18n.isInitialized) {
    return i18n;
  }
  if(Platform.OS == "web"){
    isDev = Constants.manifest.extra.devMode;
  }
  if (isDev) {
    // DevMode: lokale Übersetzungen laden
    await i18n.use(initReactI18next).init({
      lng: culture,
      fallbackLng: culture,
      resources: {
        de: { translation: mergedDe },
        en: { translation: mergedEn },
      },
      interpolation: { escapeValue: false },
      react: { useSuspense: false },
      parseMissingKeyHandler: (key: string) =>
        `Missing translation for "${key}"`,
    });
  } else {
    // ProdMode: Übersetzungen über http-Backend laden
    await i18n
      .use(Backend)
      .use(initReactI18next)
      .init({
        lng: culture,
        fallbackLng: culture,
        backend: {
          loadPath: translationUrl
            ? `${translationUrl}/{{lng}}/form.json`
            : `${meshEndpoint}/api/andp/translate/{{lng}}`,
        },
        interpolation: { escapeValue: false },
        react: { useSuspense: false },
        parseMissingKeyHandler: (key: string) =>
          `Missing translation for "${key}"`,
      });
  }
  return i18n;
}

export default i18n;
