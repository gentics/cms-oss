import { Response } from './response';

/**
 * One language entry as returned by `GET /form/translations/languages`.
 *
 * The endpoint may return either full objects with a `name` (preferred) or
 * just language codes. Consumers should be ready for both shapes.
 */
export interface FormTranslationsLanguage {
    code: string;
    name?: string;
}

/**
 * Translation payload format for `GET`/`POST /form/translations` and the
 * type-specific variant `/form/types/{type}/translations`.
 *
 * Shape: `{ "<placeholder>": { "<lang>": "<text>" } }`. On POST, only the
 * placeholders/languages that need to change need to be sent — values that
 * are not in the payload are left untouched on the server.
 */
export type FormTranslations = Record<string, Record<string, string>>;

/**
 * Response of `GET /form/translations/languages`.
 *
 * Defined permissively because the actual CMS endpoint may either wrap the
 * languages in a `languages` property or return a bare array of codes.
 */
export interface FormTranslationsLanguagesResponse extends Response {
    languages: FormTranslationsLanguage[] | string[];
}

/**
 * Response of `GET /form/translations` and `POST /form/translations` (global
 * scope), and of `GET /form/types/{type}/translations` and the matching POST
 * (type-specific scope).
 */
export interface FormTranslationsResponse extends Response {
    translations: FormTranslations;
}
