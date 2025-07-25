export interface ExternalAssetReference {
    /** The file name of the file or image. */
    name: string;
    /** Determines the type of asset type */
    fileCategory: 'file' | 'image';
    /**
     * The URL from which the asset is available to be loaded from.
     * This URL has to be reachable from the CMS Server and not from the user in order to work,
     * as the CMS itself is downloading and processing the asset directly and skips the User/Browser.
     */
    '@odata.mediaReadLink': string;
    /** The description of the asset */
    description?: string;
    /** The nice-url the asset should be assigned */
    niceUrl?: string;
    /** Alternate/Additional nice-urls for the asset */
    alternateUrls?: string[];
    /** (Object-)Properties which should be set. */
    properties?: { [key: string]: any };
}
