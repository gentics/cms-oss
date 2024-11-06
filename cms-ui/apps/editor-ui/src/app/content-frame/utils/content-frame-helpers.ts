import { File as FileModel, Folder, Form, Image as ImageModel, Node, Page } from '@gentics/cms-models';

/**
 * Given an item type, returns the key that should go in the URL to prevent state errors which
 * tend to result in the "Das Laden der letzten Seite wurde aus technischen Gründen angehalten" error.
 */
export function getTypeIdKey(type: string): string {
    switch (type) {
        case 'folder':
            return 'FOLDER_ID';
        case 'page':
            return 'PAGE_ID';
        case 'file':
        case 'image':
            return 'FILE_ID';
        default:
            return 'UNKNOWN_ID';
    }
}

/**
 * Certain URLs break without the "ITEM_ID=123" param appended to the url.
 */
export function appendTypeIdToUrl(item: Page | FileModel | Folder | Form | ImageModel | Node, url: string): string {
    let newUrl = url;
    if (typeof url === 'string' && -1 < url.indexOf('do=10010')) {
        const entityKey = getTypeIdKey(item.type);
        newUrl += `&${entityKey}=${item.id}`;
    }
    return newUrl;
}

/**
 * Because debugging the life-cycles of the nested iframes is confusing, here is some color-coded
 * logging to make things a bit easier.
 * TODO: Turn off for production or add a global environment switch
 */
export function logIFrameLifecycle(mIframe: any, message: string, color?: string): void {
    const enableLogging = false;
    if (enableLogging) {
        const iframe: HTMLIFrameElement = mIframe.hasOwnProperty('iframe') ? mIframe.iframe : mIframe;
        const location = iframe.contentWindow.location.href;
        const iframeName: string = iframe.name || iframe.id || mIframe.id;
        const frameColor = stringToColour(iframeName);
        console.log(`%c${message}: %c${iframeName} %c(${location.substr(0, 50)})`,
            `color: ${color || 'black'}`,
            `color: ${frameColor}`,
            'color: #ccc');
    }

    function stringToColour(str: string): string {
        if (!str) {
            return '#555';
        }
        // tslint:disable
        for (var i = 0, hash = 0; i < str.length; hash = str.charCodeAt(i++) + ((hash << 5) - hash));
        for (var i = 0, colour = '#'; i < 3; colour += ('00' + ((hash >> i++ * 8) & 0xFF).toString(16)).slice(-2));
        // tslint:enable
        return colour;
    }
}
