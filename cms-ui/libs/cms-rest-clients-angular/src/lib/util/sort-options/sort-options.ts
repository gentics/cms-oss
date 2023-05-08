import { PagingSortOption, EmbedListOptions } from "@gentics/cms-models";

/**
     * Stringifies the `sort` property of a `BaseListOptionsWithPaging` for use
     * as a query parameter with the GCMS REST API.
     */
export function stringifyPagingSortOptions<T>(sort: PagingSortOption<T> | PagingSortOption<T>[] | string): string {
    if (typeof sort === 'string') {
        return sort;
    }

    const sortOptions = Array.isArray(sort) ? sort : [sort];
    const sortStrings = sortOptions.map(sortOption => {
        const order = sortOption.sortOrder || '';
        return `${order}${String(sortOption.attribute)}`;
    });
    return sortStrings.join(',');
}

export function stringifyEmbedOptions<T>(options: EmbedListOptions<T>): void {
    if (options == null || typeof options !== 'object' || !Array.isArray(options.embed)) {
        return;
    }

    (options as any).embed = options.embed.join(',');
}
