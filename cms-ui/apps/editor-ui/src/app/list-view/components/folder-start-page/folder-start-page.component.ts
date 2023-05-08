import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChange } from '@angular/core';
import { EditorTab, UIMode } from '@editor-ui/app/common/models';
import { getNestedObject } from '@editor-ui/app/common/utils/get-nested-object';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { Folder, Page, StagedItemsMap } from '@gentics/cms-models';
import { isEqual } from 'lodash';
import { BehaviorSubject, Subscription, combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';

type StartPage = Page | string;
enum StartPageType {
    Internal = 'internal',
    External = 'external',
    Deleted = 'deleted',
}

/**
 * Shows the start page of a folder.
 * Provides preview, edit and reassign shortcuts to internal pages and open in new tab to external pages.
 */
@Component({
    selector: 'folder-start-page',
    templateUrl: './folder-start-page.component.html',
    styleUrls: ['./folder-start-page.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
    })
export class FolderStartPageComponent implements OnInit, OnChanges, OnDestroy {

    readonly UIMode = UIMode;

    @Input()
    public folder: Folder;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    startPage$ = new BehaviorSubject<StartPage>(null);

    get getStartPageType(): StartPageType {
        if (this.startPage$.value === StartPageType.Deleted) {
            return StartPageType.Deleted;
        } else if (!!this.startPage$.value && !!(this.startPage$.value as any).name) {
            return StartPageType.Internal;
        } else {
            return StartPageType.External;
        }
    }

    get hasStartPageInfo(): boolean {
        return !!this.getStartPageId() || !!(this.folder && this.folder.tags);
    }

    private subscriptions = new Subscription();

    constructor(
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private appState: ApplicationStateService,
        private navigationService: NavigationService,
        private contextMenuOperations: ContextMenuOperationsService,
    ) {}

    ngOnInit(): void {
        this.subscriptions.add(combineLatest([
            this.appState.select(state => state.entities.page),
            this.appState.select(state => state.entities.folder),
            this.appState.select(state => state.folder.pages.list),
            this.appState.select(state => state.folder.activeLanguage),
            this.appState.select(state => state.folder.activeFolder),
        ]).pipe(
            distinctUntilChanged(isEqual),
            debounceTime(50),
        ).subscribe(([page, folder, folderPages]) => {
            if (folder && this.folder && folder[this.folder.id]) {
                this.folder = folder[this.folder.id];
                if (this.hasStartPageInfo) {
                    this.getStartPage();
                }
            } else {
                this.getPageWithFolderItem();
            }
        }),
        );
    }

    ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        if (changes.folder) {
            if (this.hasStartPageInfo) {
                this.getStartPage();
            } else {
                this.getPageWithFolderItem();
            }
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    /**
     * Open a URL in new tab
     *
     * @param url Url to navigate
     */
    openInNewTab(url: string): void {
        window.open(url, '_blank');
    }

    /**
     * Open a page in preview mode
     *
     * @param page The page to preview
     */
    previewPage(page: Page): void {
        this.navigationService.detailOrModal(this.folder.nodeId, 'page', page.id, 'preview').navigate();
    }

    /**
     * Open a page in edit mode
     *
     * @param page The page to edit
     */
    editPage(page: Page): void {
        this.navigationService.detailOrModal(this.folder.nodeId, 'page', page.id, 'edit').navigate();
    }

    /**
     * Open the start page object property of the folder
     *
     * @param folder The folder to setup
     */
    reassignStartPage(folder: Folder): void {
        const options = { openTab: 'properties' as EditorTab, propertiesTab: 'object.startpage' };
        this.navigationService.detailOrModal(folder.nodeId, folder.type, folder.id, 'editProperties', options).navigate();
    }

    /**
     * Return the folder startPageId property
     */
    getStartPageId(): number | null {
        return this.folder ? this.folder.startPageId : null;
    }

    /**
     * Return the folder external stringValue property
     */
    getExternalPage(): string | null {
        return this.folder && this.folder.tags
            ? (getNestedObject(this.folder.tags, ['object.startpage', 'properties', 'url']) || {}).stringValue
            : null;
    }

    stagePage(page: Page, allVariations: boolean): void {
        if (this.stagingMap?.[page.globalId]?.included) {
            this.contextMenuOperations.unstagePageFromCurrentPackage(page, allVariations);
        } else {
            this.contextMenuOperations.stagePageToCurrentPackage(page, allVariations);
        }
    }

    private getPageWithFolderItem(): void {
        // Load folder with all its details
        if (!this.folder) {
            return;
        }

        this.folderActions.getItem(this.folder.id, 'folder', { nodeId: this.folder.nodeId })
            .then(folder => {
                this.folder = folder;
                if (this.hasStartPageInfo) {
                    this.getStartPage();
                }
            });
    }

    private getStartPage(): void {
        const startPageId = this.getStartPageId();
        const startPageExternalUrl = this.getExternalPage();

        if (startPageId) {
            this.getStartPageItem(startPageId);
        } else if (startPageExternalUrl) {
            this.startPage$.next(startPageExternalUrl);
        } else {
            this.startPage$.next(null);
        }
    }

    private getStartPageItem(pageId: number): void {
        const languageId = this.appState.now.folder.activeLanguage;
        let page = this.entityResolver.getEntity('page', pageId);

        if (page && !page.deleted?.by) {
            if (!page.languageVariants ||
                !page.languageVariants[languageId] ||
                (page.languageVariants[languageId] && page.languageVariants[languageId] === page.id)
            ) {
                this.startPage$.next(page);
            } else {
                this.getStartPageItem(page.languageVariants[languageId]);
            }
            return;
        }

        this.folderActions
            .getItem(pageId, 'page', { nodeId: this.folder.nodeId, langvars: true }, true)
            .then(page => {
                if (!page.languageVariants ||
                    !page.languageVariants[languageId] ||
                    (page.languageVariants[languageId] && page.languageVariants[languageId].id === page.id)
                ) {
                    this.startPage$.next(page);
                } else {
                    this.startPage$.next(page.languageVariants[languageId]);
                }
            })
            .catch(err => {
                this.startPage$.next(StartPageType.Deleted);
            });
    }
}
