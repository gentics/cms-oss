import { EmbedListOptions, PagingSortOption, Response, ResponseCode } from '@gentics/cms-models';
import { GCMSRestClientRequestError } from './errors';
import { GCMSRestClientRequest } from './models';

export function toRelativePath(path: string): string {
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

const codeToHttpCode: Record<ResponseCode, number> = {
    [ResponseCode.OK]: 200,
    [ResponseCode.NOT_FOUND]: 404,
    [ResponseCode.INVALID_DATA]: 400,
    [ResponseCode.FAILURE]: 500,
    [ResponseCode.PERMISSION]: 403,
    [ResponseCode.AUTH_REQUIRED]: 401,
    [ResponseCode.MAINTENANCE_MODE]: 503,
    [ResponseCode.NOT_LICENSED]: 501,
    [ResponseCode.LOCKED]: 503,
};

function isResponseObject(value: any): value is Response {
    return value != null
        && typeof value === 'object'
        && value.responseInfo != null
        && typeof value.responseInfo === 'object';
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function validateResponseObject(request: GCMSRestClientRequest, response: any): void {
    if (!isResponseObject(response)) {
        return;
    }

    if (response.responseInfo.responseCode !== 'OK') {
        throw new GCMSRestClientRequestError(
            response?.messages[0]?.message || response.responseInfo.responseMessage || `Request "${request.method} ${request.url}" responded with an Error-Response.`,
            request,
            codeToHttpCode[response.responseInfo.responseCode],
            null,
            response,
            null,
        );
    }
}


