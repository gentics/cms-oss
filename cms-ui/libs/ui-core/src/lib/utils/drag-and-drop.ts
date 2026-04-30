export function getDataTransfer(event: any): DataTransfer {
    if (event == null) {
        return null;
    }
    // if jQuery wrapped the event which contains the dropped file, unwrap it
    const ev: DragEvent = event.dataTransfer ? event : event.originalEvent;
    if (ev == null) {
        return null;
    }
    return ev.dataTransfer;
}

export function getEventTarget(event: any): HTMLElement {
    // if jQuery wrapped the event, unwrap it
    return (event.originalEvent ? event.originalEvent : event).target;
}

/**
 * Note: there are issues with current TypeScript lib defs for the DataTransfer interface, which
 * seems to define the `types` property as a `string[]` rather than a DOMStringList.
 * See https://github.com/Microsoft/TypeScript/issues/12069
 */
export function transferHasFiles(transfer: DataTransfer): boolean {
    if (transfer == null) {
        return false;
    }
    const types = transfer.types as any as string[] & DOMStringList;
    if (!transfer || !transfer.types) {
        return false;
    } else if (typeof types.contains === 'function') {
        return types.contains('Files');
    } else if (typeof types.indexOf === 'function') {
        return types.indexOf('Files') >= 0;
    } else if (typeof types.length === 'number') {
        for (let i = 0; i < types.length; i++) {
            if (types.item(i) === 'Files') {
                return true;
            }
        }
    }
    return false;
}

let mimeTypeSupport: boolean;
export function clientReportsMimeTypesOnDrag(): boolean {
    if (mimeTypeSupport === undefined) {
        mimeTypeSupport = 'items' in DataTransfer.prototype;
    }
    return mimeTypeSupport;
}

/**
 * If the browser does not report a MIME type, match against this value instead.
 */
export const FALLBACK_MIME_TYPE = 'unknown/unknown';

let warnedThatBrowserDoesNotProvideNumberOfItems = false;

/**
 * Returns a list of mime types in a DataTransfer if supported by the browser.
 *
 * This is a workaround for missing DataTransfer.items support in Firefox < 52
 * https://bugzilla.mozilla.org/show_bug.cgi?id=906420
 */
export function getTransferMimeTypes(transfer: DataTransfer): string[] {
    if (!transfer) {
        return [];
    } else if (transfer.items && transfer.items.length > 0) {
        return Array.from(transfer.items)
            .filter((item) => item.kind === 'file')
            .map((item) => item.type || FALLBACK_MIME_TYPE);
    } else if ('mozItemCount' in transfer) {
        return new Array((<any> transfer).mozItemCount).fill(FALLBACK_MIME_TYPE);
    } else if (!transfer.items && transfer.types.length === 1 && transfer.types[0] === 'Files') {
        // IE11
        return [FALLBACK_MIME_TYPE];
    } else {
        if (!warnedThatBrowserDoesNotProvideNumberOfItems) {
            console.warn('Client does not provide number of items during drag event');
            warnedThatBrowserDoesNotProvideNumberOfItems = true;
        }
        return [FALLBACK_MIME_TYPE];
    }
}
