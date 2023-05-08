import {
    AllowedSelectionType,
    File,
    Folder,
    Form,
    Image,
    ItemInNode,
    Node,
    Page,
    Raw,
    RepoItem,
    RepositoryBrowserDataServiceOptions,
    RepositoryBrowserSorting,
    SortField,
    Tag,
    Template
} from '@gentics/cms-models';
import { Observable } from 'rxjs';

/** The public API of {@link RepositoryBrowserDataService} that the {@link RepositoryBrowser} uses. */
export interface RepositoryBrowserDataServiceAPI {
    readonly canSubmit$: Observable<boolean>;
    readonly currentNodeId$: Observable<number>;
    readonly currentNode$: Observable<Node>;
    readonly currentNodeId: number;
    readonly currentParent: Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>;
    readonly files$: Observable<File<Raw>[]>;
    readonly filter$: Observable<string>;
    readonly folders$: Observable<Folder<Raw>[]>;
    readonly forms$: Observable<Form<Raw>[]>;
    readonly hasPermissions$: Observable<boolean>;
    readonly images$: Observable<Image<Raw>[]>;
    readonly isDisplayingFavourites$: Observable<boolean>;
    readonly isDisplayingFavouritesFolder$: Observable<boolean>;
    readonly isPickingFolder: boolean;
    readonly itemTypesToDisplay$: Observable<AllowedSelectionType[]>;
    readonly loading$: Observable<boolean>;
    readonly nodes$: Observable<Node<Raw>[]>;
    readonly pages$: Observable<Page<Raw>[]>;
    readonly parentItems$: Observable<Array<Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>>>;
    readonly search$: Observable<string>;
    readonly selected$: Observable<ItemInNode[]>;
    readonly selectedItems: ItemInNode[];
    readonly showFavourites$: Observable<boolean>;
    readonly sortOrder$: Observable<RepositoryBrowserSorting>;
    readonly startPageId$: Observable<number | undefined>;
    readonly tags$: Observable<Tag[]>;
    readonly templates$: Observable<Template<Raw>[]>;
    changeFolder(folder: number | Folder): void;
    changeNode(node: number | Node | 'favourites'): void;
    changeParent(newParent: Folder | Page | Template | Node): void;
    init(options: RepositoryBrowserDataServiceOptions): void;
    isSelected(item: RepoItem, nodeId?: number): boolean;
    selectItem(item: RepoItem): void;
    deselectItem(item: RepoItem | ItemInNode): void;
    setFilter(filter: string): void;
    setSearch(search: string): void;
    setSorting(type: AllowedSelectionType, sortBy: SortField, sortOrder: 'asc' | 'desc'): void;
    switchToFavourites(): void;
    getTotalUsageForCurrentItemsOfType(type: 'file' | 'form' | 'image' | 'page'): void;
}
