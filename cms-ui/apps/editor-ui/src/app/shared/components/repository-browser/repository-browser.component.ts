import { ChangeDetectionStrategy, Component, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { RepositoryBrowserDataServiceAPI } from '@editor-ui/app/common/models';
import { ApplicationStateService, SetListDisplayFieldsAction } from '@editor-ui/app/state';
import {
    AllowedSelection,
    AllowedSelectionType,
    Folder,
    FolderItemOrTemplateType,
    Form,
    ItemInNode,
    Language,
    Node,
    NodeFeature,
    Page,
    Raw,
    RepoItem,
    RepositoryBrowserDataServiceOptions,
    RepositoryBrowserOptions,
    RepositoryBrowserSorting,
    Template,
} from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable, Subject, combineLatest } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { RepositoryBrowserDataService } from '../../providers';

/**
 * A repository browser that allows selecting items / an item from
 * multichannelling nodes and their subfolders.
 *
 * It can be used for single or multiple selection
 * and limit the type of the allowed selection.
 *
 * Do not use this class directly, use the `RepositoryBrowserClient` service instead.
 *
 * This component forwards data from the {@link RepositoryBrowserDataService} to its child components:
 * - RepositoryBrowserBreadcrumb renders the node selector and the parent breadcrumb bar
 * - RepositoryBrowserList renders the folder contents and handles user selection of items
 *
 */
@Component({
    selector: 'repository-browser',
    templateUrl: './repository-browser.tpl.html',
    styleUrls: ['./repository-browser.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [RepositoryBrowserDataService],
})
export class RepositoryBrowser implements IModalDialog, OnInit, OnDestroy {

    /**
     * A configuration object that defines all options that are available on the repo browser.
     *
     * @example
     * {
     *     allowedTypes: ['file', 'image'],
     *     selectMultiple: true,
     *     startNode: 1234,
     *     startFolder: 5678,
     *     requiredPermissions: (selected, parent, node) => { ... }
     * }
     */
    @Input() options: RepositoryBrowserOptions;

    allowed: AllowedSelection = {};
    isPickingFolder: boolean;
    submitLabelKey = 'modal.repository_browser_submit';
    titleKey = '';
    titleParams: { [key: string]: string | number } = {};

    canSubmit$: Observable<boolean>;
    currentNodeId$: Observable<number>;
    currentNode$: Observable<Node>;
    filter$: Observable<string>;
    hasPermissions$: Observable<boolean>;
    hasPermissions = false;
    itemTypesToDisplay$: Observable<AllowedSelectionType[]>;
    isDisplayingFavourites$: Observable<boolean>;
    isDisplayingFavouritesFolder$: Observable<boolean>;
    loading$: Observable<boolean>;
    noItemsOfAnyType$: Observable<boolean>;
    nodes$: Observable<Node[]>;
    parentItems$: Observable<Array<Folder | Page | Template | Node>>;
    search$: Observable<string>;
    selected$: Observable<ItemInNode[]>;
    showFavourites$: Observable<boolean>;
    startPageId$: Observable<number | undefined>;
    pageShowPath$: Observable<boolean>;

    /** Observable for each type that emits the display fields. */
    displayFieldsForType: { [key: string]: Observable<string[]> };

    /** Observables that emit the item list for each item type. */
    observableForType: { [key: string]: Observable<RepoItem[]> };

    /** Observables that emit the sort order for each item type. */
    sortOrder$: Observable<RepositoryBrowserSorting>;

    /** If TRUE CMS entity `form` is allowed and available to be displayed in repository browser. */
    isActiveNodeFeatureForms$: Observable<boolean>;

    private destroy = new Subject<void>();

    constructor(
        private appState: ApplicationStateService,
        @Inject(RepositoryBrowserDataService) private dataService: RepositoryBrowserDataServiceAPI,
        private userSettings: UserSettingsService,
    ) { }

    ngOnInit(): void {
        this.initializeDataService();
        this.parseOptions();
        this.createTitle();
        this.setupObservables();
    }

    ngOnDestroy(): void {
        this.destroy.next();
        this.destroy.complete();
    }

    changeNode(node: number | Node | 'favourites'): void {
        return this.dataService.changeNode(node);
    }

    changeParent(newParent: Folder | Page | Template | Node): void {
        return this.dataService.changeParent(newParent);
    }

    itemClicked(item: RepoItem): void {
        if (item.type === 'folder') {
            this.dataService.changeFolder(item.id);
        } else if (item.type === 'page' && this.allowed.contenttag) {
            this.changeParent(item as Page);
        } else if (item.type === 'template' && this.allowed.templatetag) {
            this.changeParent(item as Template);
        } else if (this.dataService.isSelected(item)) {
            this.dataService.deselectItem(item);
        } else if (this.allowed[item.type.toLowerCase()]) {
            this.dataService.selectItem(item);
        }
    }

    selectItem(item: RepoItem): void {
        return this.dataService.selectItem(item);
    }

    deselectItem(item: RepoItem): void {
        return this.dataService.deselectItem(item);
    }

    isSelected(item: RepoItem): Observable<boolean> {
        return this.selected$.pipe(
            map(selectedItems => {
                return (selectedItems || []).some(sel => sel.id === item.id && sel.type === item.type);
            }),
        );
    }

    toggleItemSelection(item: RepoItem, to: boolean): void {
        if (to) {
            this.selectItem(item);
        } else {
            this.deselectItem(item);
        }
    }

    setFilter(filter: string): void {
        return this.dataService.setFilter(filter);
    }

    setSearch(search: string): void {
        return this.dataService.setSearch(search);
    }

    submitClicked(): void {
        let items: ItemInNode[];
        if (this.isPickingFolder) {
            const currentParent = this.dataService.currentParent;
            const nodeId = this.dataService.currentNodeId;
            const folder = Object.assign({}, currentParent, { nodeId });
            items = [folder];
        } else {
            items = [...this.dataService.selectedItems];
        }

        if (!items.length) {
            return;
        }

        if (this.options.selectMultiple) {
            this.closeFn(items);
        } else {
            this.closeFn(items[0]);
        }
    }

    updateDisplayFields(type: FolderItemOrTemplateType, fields: string[]): void {
        this.userSettings.setDisplayFields(type, fields);
        this.appState.dispatch(new SetListDisplayFieldsAction(type, fields || []));
    }

    closeFn(val: ItemInNode | ItemInNode[]): void { }
    cancelFn(val?: any): void { }

    registerCloseFn(close: (val: ItemInNode | ItemInNode[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    onPageLanguageIconClicked(data: { page: Page<Raw>; language: Language; }): void {
        if (!this.hasPermissions) {
            return;
        }
        const pageVariant = Object.values(data.page.languageVariants).find((variant: Page<Raw>) => variant.language === data.language.code);
        if (!pageVariant) {
            return;
        }
        this.selectItem(pageVariant);
        this.submitClicked();
    }

    onFormLanguageIconClicked(data: { form: Form<Raw>; language: Language; }): void {
        if (!this.hasPermissions) {
            return;
        }
        this.selectItem(data.form);
        this.submitClicked();
    }

    private initializeDataService(): void {
        const options = normalizeDataServiceOptions(this.options);
        this.dataService.init(options);

        this.allowed = options.allowedSelection;
        this.isPickingFolder = this.dataService.isPickingFolder;
    }

    private parseOptions(): void {
        const options = this.options;
        if (options.submitLabel) {
            this.submitLabelKey = options.submitLabel;
        }
    }

    private setupObservables(): void {

        this.isActiveNodeFeatureForms$ = combineLatest([
            this.appState.select(state => state.folder.activeNode),
            this.appState.select(state => state.features.nodeFeatures),
        ]).pipe(
            map(([activeNodeId, nodeFeatures]) => {
                const activeNodeFeatures = nodeFeatures[activeNodeId];
                const isActiveFeatureForms = Array.isArray(activeNodeFeatures) && activeNodeFeatures.includes(NodeFeature.FORMS);
                return isActiveFeatureForms;
            }),
        );

        const dataService = this.dataService;

        this.canSubmit$ = dataService.canSubmit$;
        this.currentNodeId$ = dataService.currentNodeId$;
        this.currentNode$ = dataService.currentNode$;
        this.filter$ = dataService.filter$;
        this.hasPermissions$ = dataService.hasPermissions$;
        this.itemTypesToDisplay$ = dataService.itemTypesToDisplay$;
        this.isDisplayingFavourites$ = dataService.isDisplayingFavourites$;
        this.isDisplayingFavouritesFolder$ = dataService.isDisplayingFavouritesFolder$;
        this.loading$ = dataService.loading$;
        this.nodes$ = dataService.nodes$;
        this.parentItems$ = dataService.parentItems$;
        this.search$ = dataService.search$;
        this.selected$ = dataService.selected$;
        this.showFavourites$ = dataService.showFavourites$;
        this.sortOrder$ = dataService.sortOrder$;
        this.startPageId$ = dataService.startPageId$;
        this.pageShowPath$ = this.appState.select(state => state.folder.pages.showPath);

        this.observableForType = {
            folder: dataService.folders$,
            form: dataService.forms$,
            page: dataService.pages$,
            file: dataService.files$,
            image: dataService.images$,
            template: dataService.templates$,
            contenttag: dataService.tags$,
            templatetag: dataService.tags$,
        };

        const allItemTypes$ = Object.keys(this.observableForType).map(k => this.observableForType[k]);
        this.noItemsOfAnyType$ = combineLatest([
            combineLatest(allItemTypes$),
            this.isActiveNodeFeatureForms$,
        ]).pipe(
            map(([itemArrays, isActiveNodeFeatureForms]) => {
                return isActiveNodeFeatureForms ? itemArrays : itemArrays.filter(t => t.some(i => i.type !== 'form'));
            }),
            map(([itemArrays]) => Array.isArray(itemArrays) && itemArrays.every(array => !Array.isArray(array))),
        );

        this.displayFieldsForType = {
            folder: this.appState.select(state => state.folder.folders.displayFields),
            form: this.appState.select(state => state.folder.forms.displayFields),
            page: this.appState.select(state => state.folder.pages.displayFields),
            file: this.appState.select(state => state.folder.files.displayFields),
            image: this.appState.select(state => state.folder.images.displayFields),
        };

        this.hasPermissions$.pipe(
            takeUntil(this.destroy),
        ).subscribe(hasPermissions => this.hasPermissions = hasPermissions);
    }

    private createTitle(): void {
        if (this.options.title) {
            this.titleKey = this.options.title;
            return;
        }

        const selectMultiple = this.options.selectMultiple;
        const types: string[] = Object.keys(this.allowed)
            .filter(type => (<any> this.allowed)[type])
            .filter(part => !!part)
            .sort();
        const typeList = types.join(',');

        if (types.length === 1) {
            this.titleKey = selectMultiple ?
                'modal.repository_browser_title_multiple' :
                'modal.repository_browser_title_single';
            this.titleParams = { _type: types[0], count: selectMultiple ? 2 : 1 };
        } else if (typeList === 'file,image,page') {
            this.titleKey = 'modal.repository_browser_title_file_image_page';
        } else {
            console.error('Repository browser: No translation for ' + typeList);
        }
    }

}

function normalizeAllowedSelectionType(allowedTypes: AllowedSelection | AllowedSelectionType | AllowedSelectionType[]): AllowedSelection {
    if (typeof allowedTypes === 'string') {
        return { [allowedTypes]: true };
    } else if (Array.isArray(allowedTypes)) {
        const allowed: AllowedSelection = {};
        allowedTypes.forEach(t => allowed[t] = true);
        return allowed;
    } else if (typeof allowedTypes === 'object') {
        return Object.assign({}, allowedTypes);
    } else {
        throw new TypeError('allowedSelection is invalid');
    }
}

function normalizeDataServiceOptions(repoOptions: RepositoryBrowserOptions): RepositoryBrowserDataServiceOptions {
    const {
        onlyInCurrentNode,
        contentLanguage,
        requiredPermissions,
        selectMultiple,
        startFolder,
        startNode,
        includeMlId,
    } = repoOptions;

    const result: RepositoryBrowserDataServiceOptions = {
        allowedSelection: normalizeAllowedSelectionType(repoOptions.allowedSelection),
        contentLanguage,
        onlyInCurrentNode,
        requiredPermissions,
        selectMultiple,
        startFolder,
        startNode,
        includeMlId,
    };
    return result;
}

