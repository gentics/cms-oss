/**
 * GCMSUI component `item-list-row` has different states
 * causing different component behavior.
 */
export enum ItemListRowMode {
    DEFAULT = 'DEFAULT',
    SELECT = 'SELECT',
}

export type UsageType =
| 'page'
| 'variant'
| 'folder'
| 'file'
| 'image'
| 'template'
| 'tag';

export const USAGE_TYPES: UsageType[] = [
    'page',
    'variant',
    'folder',
    'file',
    'image',
    'template',
    'tag',
];

export type LinkType =
| 'linkedPage'
| 'linkedFile'
| 'linkedImage';

export const LINK_TYPES: LinkType[] = [
    'linkedPage',
    'linkedFile',
    'linkedImage',
];
