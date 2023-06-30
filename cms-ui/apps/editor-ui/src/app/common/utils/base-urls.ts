// The URLs exported here are all absolute so they can be used in an iframe, but relative to the UI folder.
// A location of server.com/customer-prefix/Content.Node/ui/ will export:
//     ALOHAPAGE_URL as /customer-prefix/alohapage
//     API_BASE_URL as /customer-prefix/rest
//     CONTENTNODE_URL as /customer-prefix/Content.Node
//     IMAGESTORE_URL as /customer-prefix/GenticsImageStore

/** @internal */

interface ResourceLocations {
    alohaPage: string;
    imageStore: string;
    restAPI: string;
}

export function urlsRelativeTo(locationPathname: string): ResourceLocations {
    const contentNode = locationPathname.replace(/\/+[^/]+\/+[^/]*$/, '');
    const base = contentNode.replace(/\/[^/]*$/, '');

    return {
        alohaPage: base + '/alohapage',
        imageStore: base + '/GenticsImageStore',
        restAPI: base + '/rest',
    };
}

const paths = urlsRelativeTo(location.pathname);

/** Absolute path of the REST API */
export const API_BASE_URL = paths.restAPI;

/** Absolute path of Aloha endpoint to edit pages */
export const ALOHAPAGE_URL = paths.alohaPage;

/** Absolute path of the Gentics Image Store */
export const IMAGESTORE_URL = paths.imageStore;
