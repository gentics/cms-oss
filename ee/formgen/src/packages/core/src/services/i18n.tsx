import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import Backend, { HttpBackendOptions } from 'i18next-http-backend';
import Constants from 'expo-constants';

// Lokale (Mock-)Translations – werden in Production-Builds bzw. wenn der Ordner
// local_dev_translations/ fehlt auf devTranslations.stub.ts gemappt (siehe
// metro.dev-translations.js), sodass der Ordner dann nicht existieren muss.
import { mergedDe, mergedEn } from './devTranslations';
import { Platform } from 'react-native';
import { PreviewInformation } from '../../../feature-formgen/src/types/preview';
import { RequestCallback } from 'i18next-http-backend';

export async function initI18nForms(
  meshEndpoint: string,
  formId: string,
  pluginName: string,
  isDev = false,
  previewInfo?: PreviewInformation,
  culture: string = 'de', // Standard: 'de'
){
  const backendUrl = `${meshEndpoint}/api/${pluginName}/translations/${formId}/{{lng}}`;
  return initI18n(
    backendUrl,
    isDev,
    previewInfo,
    culture
  );
}

export async function initI18nMicroApps( 
  meshEndpoint: string,
  isDev = false,
  culture: string = 'de', // Standard: 'de'
){
  const backendUrl = `${meshEndpoint}/api/andp/translate/{{lng}}`;
  return initI18n(
    backendUrl,
    isDev,
    undefined,
    culture
  );
}

/**
 * Initialisiert i18n.
 * @param backendUrl -> loads tranlations from this url
 * @param isDev Wenn true => Lokale Datei statt http-Backend
 * @param culture Der zu verwendende Sprachcode (z.B. 'en' oder 'de')
 */
async function initI18n(
  backendUrl: string,
  isDev = false,
  previewInfo?: PreviewInformation,
  culture: string = 'de', // Standard: 'de'
) {
  if (i18n.isInitialized) {
    return i18n;
  }
  if(Platform.OS == "web"){
    isDev = Constants.manifest.extra.devMode;
  }
  if (previewInfo != null) {
    // Preview mode
    await i18n
      .use(Backend)
      .use(initReactI18next)
      .init({
        lng: culture,
        showSupportNotice: false,
        supportedLngs: previewInfo.availableLanguages,
        partialBundledLanguages: true,
        preload: previewInfo.availableLanguages,
        maxRetries: 0,
        backend: {
          request: (
            options: HttpBackendOptions,
            url: string,
            payload: {} | string,
            callback: RequestCallback
          ): void => {
            const targetUrl = new URL(url, window.location as any);
            if (previewInfo.cmsSid) {
              targetUrl.searchParams.set('sid', `${previewInfo.cmsSid}`);
            }
            fetch(targetUrl.toString(), {
              method: 'GET',
            })
              .then((res) => {
                if (!res.ok) {
                  throw new Error();
                }
                return res.json();
              })
              .then((data) => {
                callback(null, {
                  status: 200,
                  data: data,
                });
              })
              .catch((err) => {
                console.error(`Error while loading translations from URL "${url}"`);
                const parts = url.split('/')
                const lang = parts[parts.length - 1];

                if (lang === 'de') {
                  console.debug(`Fallback to local "${lang}" translations`);
                  callback(null, {
                    status: 200,
                    data: mergedDe,
                  });
                } else if (lang === 'en') {
                  console.debug(`Fallback to local "${lang}" translations`);
                  callback(null, {
                    status: 200,
                    data: mergedEn,
                  });
                } else {
                  callback(err, null);
                }
              });
          },
          loadPath: `/rest/form/types/${previewInfo.formType}/translations/{{lng}}`,
        },
        debug: isDev,
        interpolation: { escapeValue: false },
        react: { useSuspense: false },
        parseMissingKeyHandler: (key: string) =>
          `Missing translation for "${key}"`,
      });
  } else if (isDev) {
    // DevMode: lokale Übersetzungen laden
    await i18n.use(initReactI18next).init({
      lng: culture,
      fallbackLng: culture,
      showSupportNotice: false,
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
        showSupportNotice: false,
        backend: {
          loadPath: `${backendUrl}`,
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
