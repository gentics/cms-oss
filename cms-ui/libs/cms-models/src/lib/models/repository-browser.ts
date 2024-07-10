import { Form } from './cms-form';
import { File } from './file';
import { Folder } from './folder';
import { Image } from './image';
import { Item } from './item';
import { Page } from './page';
import { SortField } from './request';
import { Tag } from './tag';
import { MarkupLanguageType } from './tag-part-types';
import { Template } from './template';
import { Raw } from './type-util';

/** Represents an item in a node, as returned by the RepositoryBrowser. */
export type ItemInNode<T extends Item<Raw> = Item<Raw>> = T & {
    /** The node, in which the item is located (added by the RepositoryBrowser). */
    nodeId: number;
};

/** Represents an Tag inside a container, as returned by the RepositoryBrowser. */
export type TagInContainer<T extends Page<Raw> | Template<Raw> = Page<Raw> | Template<Raw>> = Tag & {
    /** The parent container (reference added by the RepositoryBrowser). */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    __parent__: T;
};

export type RepoItem = Folder<Raw> | Form<Raw> | Page<Raw> | File<Raw> | Image<Raw> | Template<Raw> | Tag;

export interface AllowedSelection {
    page?: boolean;
    folder?: boolean;
    form?: boolean;
    image?: boolean;
    file?: boolean;
    template?: boolean;
    contenttag?: boolean;
    templatetag?: boolean;
}

export interface RepositoryBrowserSorting {
    folder: { field: SortField; order: 'asc' | 'desc'; };
    form: { field: SortField; order: 'asc' | 'desc'; };
    page: { field: SortField; order: 'asc' | 'desc'; };
    file: { field: SortField; order: 'asc' | 'desc'; };
    image: { field: SortField; order: 'asc' | 'desc'; };
    template: { field: SortField; order: 'asc' | 'desc'; };
    contenttag: { field: SortField; order: 'asc' | 'desc'; };
    templatetag: { field: SortField; order: 'asc' | 'desc'; };
}

export type AllowedItemSelectionType = 'page' | 'folder' | 'form' | 'image' | 'file' | 'template';
export type AllowedTagSelectionType = 'contenttag' | 'templatetag';
export type AllowedSelectionType = AllowedItemSelectionType | AllowedTagSelectionType;

export interface SerializableRepositoryBrowserOptions {
    allowedSelection: AllowedSelectionType | AllowedSelectionType[];
    contentLanguage?: string;
    onlyInCurrentNode?: boolean;
    selectMultiple: boolean;
    startNode?: number;
    startFolder?: number;
    submitLabel?: string;
    title?: string;
    includeMlId?: MarkupLanguageType[];
}

export interface AllowedSelectionTypeMap {
    folder: ItemInNode<Folder<Raw>>;
    form: ItemInNode<Form<Raw>>;
    page: ItemInNode<Page<Raw>>;
    file: ItemInNode<File<Raw>>;
    image: ItemInNode<Image<Raw>>;
    template: ItemInNode<Template<Raw>>;
    contenttag: TagInContainer<Page<Raw>>;
    templatetag: TagInContainer<Template<Raw>>;
}
