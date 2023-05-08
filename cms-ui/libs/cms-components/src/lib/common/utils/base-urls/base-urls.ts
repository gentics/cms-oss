// The URLs exported here are all absolute so they can be used in an iframe, but relative to the UI folder.
// A location of server.com/customer-prefix/Content.Node/ui/ will export:
//     ALOHAPAGE_URL as /customer-prefix/alohapage
//     API_BASE_URL as /customer-prefix/rest
//     CONTENTNODE_URL as /customer-prefix/Content.Node
//     IMAGESTORE_URL as /customer-prefix/GenticsImageStore

/** @internal */
export function urlsRelativeTo(locationPathname: string): {
        alohaPage: string;
        contentNode: string;
        imageStore: string;
        restAPI: string;
        guides: string;
        changelog: string; } {
    const contentNode = locationPathname.replace(/\/+[^\/]+\/+[^/]*$/, '');
    const base = contentNode.replace(/\/[^\/]*$/, '');
    return {
        alohaPage: base + '/alohapage',
        contentNode: contentNode !== '/' && contentNode || '/.Node',
        imageStore: base + '/GenticsImageStore',
        restAPI: base + '/rest',
        guides: base + '/guides',
        changelog: base + '/changelog',
    };
}

const paths = urlsRelativeTo(location.pathname);

/** Absolute path of the REST API */
export const API_BASE_URL = paths.restAPI;

/** Absolute path of Aloha endpoint to edit pages */
export const ALOHAPAGE_URL = paths.alohaPage;

/** Absolute path of Content.Node PHP */
export const CONTENTNODE_URL = paths.contentNode;

/** Absolute path of the Gentics Image Store */
export const IMAGESTORE_URL = paths.imageStore;

/** Absolute path of the Gentics CMS Guides */
export const GUIDES_URL = paths.guides;

/** Absolute path of the Gentics CMS Changelog */
export const CHANGELOG_URL = paths.changelog;
