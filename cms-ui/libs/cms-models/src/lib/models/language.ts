
/**
 * A language object as returned from the /node/languages endpoint:
 * http://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_NodeResource.html#resource_NodeResource_languages_GET
 */
export interface Language {
    /** The numeric ID of the language */
    id: number;

    /** The global ID of the language */
    globalId?: string;

    /** A 2-letter language code like "en" / "de" / "it". */
    code: string;

    /** A human-readable name of the language like "Italiano (Italian)" */
    name: string;
}
