/**
 * DTOs — direkt von der Gentics CMS REST-API.
 * Bitte NICHT direkt in der UI verwenden — vorher in ViewModels transformieren.
 */

/** Standard-Gentics-Response-Hülle. */
export interface GcmsResponseInfo {
  responseCode: 'OK' | 'NOTFOUND' | 'PERMISSION' | 'INVALIDDATA' | 'FAILURE' | string;
  responseMessage?: string;
}

export interface GcmsBaseResponse {
  responseInfo: GcmsResponseInfo;
}

/**
 * Antwort von GET /rest/form/translations/languages.
 * Die genaue Form ist im CMS leicht variabel — wir akzeptieren mehrere Varianten
 * und normalisieren im Service. Häufige Formen sind:
 *   { responseInfo, languages: [{ code, name }] }
 *   { responseInfo, languages: ['de', 'en'] }
 *   ['de', 'en']
 */
export type LanguagesResponseDto =
  | (GcmsBaseResponse & { languages: LanguageDto[] | string[] })
  | LanguageDto[]
  | string[];

export interface LanguageDto {
  code: string;
  name?: string;
}

/**
 * Übersetzungs-Payload: { "<placeholder>": { "<lang>": "<text>" } }.
 * Sowohl GET- als auch POST-Format. Der Response-Wrapper umhüllt das im "translations"-Feld
 * (im Mock spiegeln wir beide Varianten — Wrapper für GET, raw für POST).
 */
export type TranslationsPayloadDto = Record<string, Record<string, string>>;

/** Antwort von GET /rest/form/translations bzw. /rest/form/types/{type}/translations. */
export type TranslationsResponseDto =
  | (GcmsBaseResponse & { translations: TranslationsPayloadDto })
  | TranslationsPayloadDto;

/**
 * Antwort des Formulartypen-Endpoints. Auch hier akzeptieren wir mehrere Formen;
 * im echten CMS ist der Endpoint noch zu klären (vermutlich `/rest/form/types`).
 */
export type FormTypesResponseDto =
  | (GcmsBaseResponse & { types: FormTypeDto[] | string[] })
  | FormTypeDto[]
  | string[];

export interface FormTypeDto {
  /** Key, der in der Endpoint-URL verwendet wird, z. B. "andp". */
  key: string;
  /** Sprechende Bezeichnung für die UI. */
  name?: string;
  /** Optionale Beschreibung. */
  description?: string;
}
