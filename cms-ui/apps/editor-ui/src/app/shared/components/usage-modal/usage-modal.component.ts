import { Component, OnInit } from '@angular/core';
import {
    EditMode,
    File as FileModel,
    Folder,
    Image,
    InheritableItem,
    Item,
    Page,
    Template,
} from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { Version } from '../../../core/providers/user-settings/version.class';
import { ApplicationStateService, UsageActionsService } from '../../../state';

@Component({
    selector: 'usage-modal',
    templateUrl: './usage-modal.tpl.html',
    styleUrls: ['./usage-modal.scss'],
})
export class UsageModalComponent implements IModalDialog, OnInit {

    item: Item;
    nodeId: number;
    currentLanguageId: number;
    supportsExtendedUsage = false;

    linkedPages$: Observable<Page[]>;
    linkedFiles$: Observable<FileModel[]>;
    linkedImages$: Observable<Image[]>;
    pages$: Observable<Page[]>;
    files$: Observable<FileModel[]>;
    folders$: Observable<Folder[]>;
    images$: Observable<Image[]>;
    templates$: Observable<Template[]>;
    variants$: Observable<Page[]>;
    fetching$: Observable<boolean>;
    usageCount$: Observable<number>;
    usageEmpty$: Observable<boolean>;
    linksCount$: Observable<number>;
    linksEmpty$: Observable<boolean>;

    constructor(
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private navigationService: NavigationService,
        private usageActions: UsageActionsService,
    ) {

        const currentVersion = Version.parse(this.appState.now.ui.cmpVersion && this.appState.now.ui.cmpVersion.version);
        const targetVersion = Version.parse('5.29.0');
        this.supportsExtendedUsage = currentVersion.satisfiesMinimum(targetVersion);

        const usageState$ = this.appState.select(state => state.usage);
        this.linkedPages$ = usageState$.pipe(
            map(state => state.linkedPages.map(id => this.entityResolver.getPage(id))),
        );
        this.linkedFiles$ = usageState$.pipe(
            map(state => state.linkedFiles.map(id => this.entityResolver.getFile(id))),
        );
        this.linkedImages$ = usageState$.pipe(
            map(state => state.linkedImages.map(id => this.entityResolver.getImage(id))),
        );
        this.files$ = usageState$.pipe(
            map(state => state.files.map(id => this.entityResolver.getFile(id))),
        );
        this.folders$ = usageState$.pipe(
            map(state => state.folders.map(id => this.entityResolver.getFolder(id))),
        );
        this.images$ = usageState$.pipe(
            map(state => state.images.map(id => this.entityResolver.getImage(id))),
        );
        this.templates$ = usageState$.pipe(
            map(state => state.templates.map(id => this.entityResolver.getTemplate(id))),
        );
        this.variants$ = usageState$.pipe(
            map(state => state.variants.map(id => this.entityResolver.getPage(id))),
            map(pages => this.groupPagesByLanguage(pages, this.currentLanguageId)),
        );
        this.pages$ = usageState$.pipe(
            map(state => {
                const pagesAndTags = state.pages.concat(state.tags);
                return pagesAndTags.map(id => this.entityResolver.getPage(id));
            }),
            map(pages => this.groupPagesByLanguage(pages, this.currentLanguageId)),
        )
        this.fetching$ = usageState$.pipe(
            map(state => state.fetching),
        );

        this.usageCount$ = this.sumObservable(
            this.files$,
            this.folders$,
            this.images$,
            this.templates$,
            this.variants$,
            this.pages$,
        );

        this.linksCount$ = this.sumObservable(
            this.linkedPages$,
            this.linkedImages$,
            this.linkedFiles$,
        );

        this.usageEmpty$ = combineLatest([
            this.fetching$,
            this.usageCount$,
        ]).pipe(
            map((result: any[]) => {
                const [fetching, count] = result;
                return fetching ? false : 0 === count;
            }),
        );

        this.linksEmpty$ = combineLatest([
            this.fetching$,
            this.linksCount$,
        ]).pipe(
            map((result: any[]) => {
                const [fetching, count] = result;
                return fetching ? false : 0 === count;
            }),
        );
    }

    ngOnInit(): void {
        this.usageActions.getUsage(this.item.type, this.item.id, this.nodeId);
    }

    /**
     * The usage APIs return language variants as distinct rows, rather than grouping them in some way. In the old UI
     * each language is thus displayed on its own row. We want to group language variants into a single row and
     * just indicate the languages with an "en", "de" icon.
     */
    groupPagesByLanguage(pages: Page[], currentLanguageId: number = -1): Page[] {
        if (this.appState.now.folder.activeNodeLanguages.list.length < 1) {
            // The current node does not have multiple languages configured, so skip this step.
            return pages;
        }
        const languageGroups: { [contentSetId: number]: Page[] } = {};
        pages.forEach(page => {
            if (!languageGroups[page.contentSetId]) {
                languageGroups[page.contentSetId] = [];
            }
            languageGroups[page.contentSetId].push(page);
        });

        const groupedPages: Page[] = [];
        for (const contentSetId in languageGroups) {
            if (languageGroups.hasOwnProperty(contentSetId)) {
                const pages = languageGroups[contentSetId];
                let primaryPage: Page;
                if (pages.length === 1) {
                    primaryPage = pages[0];
                } else {
                    primaryPage = this.getPrimaryPage(pages, currentLanguageId);
                }
                const variants = pages.filter(p => p !== primaryPage);
                primaryPage.languageVariants = {
                    [primaryPage.contentGroupId]: primaryPage.id,
                };
                variants.forEach(variant => {
                    primaryPage.languageVariants[variant.contentGroupId] = variant.id;
                });
                groupedPages.push(primaryPage);
            }
        }
        return groupedPages;
    }

    /**
     * Handle the item being clicked.
     */
    itemClicked(item: InheritableItem): void {
        const editMode: EditMode = item.type === 'page' ? EditMode.PREVIEW : EditMode.EDIT_PROPERTIES;
        this.navigationService
            .detailOrModal(item.inheritedFromId, item.type, item.id, editMode)
            .navigate();
        this.closeFn(true);
    }

    /**
     * Given a number of Observable streams of arrays, this returns an Observable of the sum of the number
     * of items in all those streams.
     */
    private sumObservable(...streams: Array<Observable<any[]>>): Observable<number> {
        return combineLatest([...streams]).pipe(
            map((lists: Array<any[]>) => {
                return lists.reduce((sum: number, list: any[]) => sum + (list ? list.length : 0), 0);
            }),
        );
    }

    /**
     * Given an array of pages, returns that page which is in the language specified by
     * languageId, or if no matches, returns the first.
     */
    private getPrimaryPage(pages: Page[], languageId: number): Page {
        let primaryPage: Page;
        const currentLanguagePages = pages.filter(p => p.contentGroupId === languageId);
        if (currentLanguagePages.length === 1) {
            primaryPage = currentLanguagePages[0];
        } else {
            primaryPage = pages[0];
        }
        return primaryPage;
    }

    closeFn(val: boolean): void { }
    cancelFn(): void {}

    registerCloseFn(close: (val: boolean) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}
