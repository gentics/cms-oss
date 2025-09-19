import { EmbedListOptions, PagingSortOption, Response, ResponseCode } from '@gentics/cms-models';
import { GCMSRestClientRequestError } from './errors';
import { GCMSRestClientRequestData } from './models';

export function toRelativePath(path: string): string {
    if (/^https?:\/\//.test(path)) {
        return path;
    }
    if (!path.startsWith('/')) {
        return `/${path}`;
    }
    return path;
}

export function trimTrailingSlash(path: string): string {
    if (!path.endsWith('/')) {
        return path;
    }
    return path.substring(0, path.length - 1);
}

export function parseJSONSafe(data: any): any {
    if (data == null || typeof data !== 'string') {
        return data;
    }
    try {
        return JSON.parse(data);
    } catch (err) {
        return data;
    }
}

type SortOptions<T> = {
    sort?: PagingSortOption<T> | PagingSortOption<T>[] | string;
}

/**
 * Stringifies the `sort` property of a `BaseListOptionsWithPaging` for use
 * as a query parameter with the GCMS REST API.
 */
export function stringifyPagingSortOptions<T>(options?: SortOptions<T>): void {
    if (options == null || typeof options !== 'object' || typeof options.sort === 'string') {
        return;
    }

    const sortOptions = Array.isArray(options.sort) ? options.sort : [options.sort];
    const sortStrings = sortOptions
        .filter(val => val != null && (typeof val === 'string' || typeof val === 'object'))
        .map(sortOption => {
            if (typeof sortOption === 'string') {
                return sortOption;
            }

            const order = sortOption.sortOrder || '';
            return `${order}${String(sortOption.attribute)}`;
        });

    if (sortStrings.length > 0) {
        options.sort = sortStrings.join(',');
    }
}

export function stringifyEmbedOptions<T>(options: EmbedListOptions<T>): void {
    if (options == null || typeof options !== 'object' || !Array.isArray(options.embed)) {
        return;
    }

    (options as any).embed = options.embed.join(',');
}

function isResponseObject(value: any): value is Response {
    return value != null
        && typeof value === 'object'
        && value.responseInfo != null
        && typeof value.responseInfo === 'object';
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function validateResponseObject(
    request: GCMSRestClientRequestData,
    response: any,
    statusCode: number,
): null | GCMSRestClientRequestError {
    if (!isResponseObject(response)) {
        return null;
    }

    if (response.responseInfo.responseCode === ResponseCode.OK) {
        return;
    }

    // some responses contain no messages
    response.messages = response.messages || [];
    throw new GCMSRestClientRequestError(
        response?.messages[0]?.message || response.responseInfo.responseMessage || `Request "${request.method} ${request.url}" responded with an Error-Response.`,
        request,
        statusCode,
        null,
        response,
        null,
    );
}
