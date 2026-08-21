
/**
 * A language object as returned from the /i18n/get endpoint:
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_I18nResource.html
 */
export interface I18nLanguage {
    /** A 2-letter language code like "en" / "de" / "it". */
    code: string;

    /** A human-readable name of the language like "Italiano (Italian)" */
    name: string;
}
