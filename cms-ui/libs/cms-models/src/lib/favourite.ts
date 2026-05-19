export type FavouriteType = 'folder' | 'file' | 'page' | 'image';
export type FavouriteTypePlural = 'folders' | 'files' | 'pages' | 'images';
export const FAVOURITE_TYPES: FavouriteType[] = ['folder', 'file', 'page', 'image'];
export const FAVOURITE_TYPES_PLURAL: FavouriteTypePlural[] = ['folders', 'files', 'pages', 'images'];

/**
 * Used to store an item as a favourite in the user settings.
 */
export interface Favourite {
    type: FavouriteType;
    id: number;
    name: string;
    globalId: string;
    nodeId: number;
}

/**
 * A favourite with additional details used for display.
 */
export interface FavouriteWithDisplayDetails extends Favourite {
    nodeName: string;
    breadcrumbs: string[];
}

/**
 * An item, which can be marked as a Favourite.
 *
 * This interface contains a subset of the properties of an Item.
 */
export interface Starrable {
    id: number;
    type: string;
    globalId: string;
    name: string;
    nodeId?: number;
}
