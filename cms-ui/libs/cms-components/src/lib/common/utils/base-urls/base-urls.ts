// The URLs exported here are all absolute so they can be used in an iframe, but relative to the UI folder.
// A location of server.com/customer-prefix/Content.Node/ui/ will export:
//     ALOHAPAGE_URL as /customer-prefix/alohapage
//     API_BASE_URL as /customer-prefix/rest
//     IMAGESTORE_URL as /customer-prefix/GenticsImageStore

interface UrlCollection {
    alohaPage: string;
    imageStore: string;
    restAPI: string;
    guides: string;
    changelog: string;
    formgeneratorRestAPI: string;
}

/** @internal */
export function urlsRelativeTo(locationPathname: string): UrlCollection {
    const contentNode = locationPathname.replace(/\/+[^/]+\/+[^/]*$/, '');
    const base = contentNode.replace(/\/[^/]*$/, '');
    return {
        alohaPage: base + '/alohapage',
        imageStore: base + '/GenticsImageStore',
        restAPI: base + '/rest',
        guides: base + '/guides',
        changelog: base + '/changelog',
        formgeneratorRestAPI: base + '/rest/proxy/fg',
    };
}

const paths = urlsRelativeTo(location.pathname);

/** Absolute path of the REST API */
export const API_BASE_URL = paths.restAPI;

/** Absolute path of Aloha endpoint to edit pages */
export const ALOHAPAGE_URL = paths.alohaPage;

/** Absolute path of the Gentics Image Store */
export const IMAGESTORE_URL = paths.imageStore;

/** Absolute path of the Gentics CMS Guides */
export const GUIDES_URL = paths.guides;

/** Absolute path of the Gentics CMS Changelog */
export const CHANGELOG_URL = paths.changelog;

/**
 * Absolute path of Content.Node PHP FormGenerator plugin endpoint.
 * @deprecated Feature is not available anymore, but here for legacy reasons.
 */
export const CONTENTNODE_FORMGENERATOR_URL = paths.formgeneratorRestAPI;
