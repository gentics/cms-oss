import { Observable } from 'rxjs';
import {
    File,
    Folder,
    Form,
    Image,
    Item,
    MarkupLanguageType,
    Node,
    Page,
    Raw,
    SortField,
    Tag,
    Template,
} from '../models';


/** Represents an item in a node, as returned by the RepositoryBrowser. */
export type ItemInNode<T extends Item<Raw> = Item<Raw>> = T & {
    /** The node, in which the item is located (added by the RepositoryBrowser). */
    nodeId: number;
};

/** Represents an Tag inside a container, as returned by the RepositoryBrowser. */
export type TagInContainer<T extends Page<Raw> | Template<Raw> = Page<Raw> | Template<Raw>> = Tag & {
    /** The parent container (reference added by the RepositoryBrowser). */
    __parent__: T;
};

export type AllowedItemSelectionType = 'page' | 'folder' | 'form' | 'image' | 'file' | 'template';
export type AllowedTagSelectionType = 'contenttag' | 'templatetag';
export type AllowedSelectionType = AllowedItemSelectionType | AllowedTagSelectionType;

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

export interface RepositoryBrowserDataServiceOptions {
    allowedSelection: AllowedSelection;
    contentLanguage?: string;
    onlyInCurrentNode?: boolean;
    selectMultiple: boolean;
    startNode?: number;
    startFolder?: number;
    includeMlId?: MarkupLanguageType[];
    /** Function that can be passed in to checks for permissions on an item */
    requiredPermissions?(selected: ItemInNode[], parent: Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>, node: Node<Raw>): Observable<boolean>;
}

export interface RepositoryBrowserOptions {
    allowedSelection: AllowedSelectionType | AllowedSelectionType[];
    contentLanguage?: string;
    onlyInCurrentNode?: boolean;
    selectMultiple: boolean;
    startNode?: number;
    startFolder?: number;
    submitLabel?: string;
    title?: string;
    includeMlId?: MarkupLanguageType[];
    /** Function that can be passed in to checks for permissions on an item */
    requiredPermissions?(
        selected: ItemInNode[],
        parent: Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>,
        node: Node<Raw>,
        currentContentLanguage?: string,
    ): Observable<boolean>;
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
