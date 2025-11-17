import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { Language, Page } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { LanguageVariantMap } from '../../../common/models';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService } from '../../../state';

/**
 * A dialog used to indicate which variants of a page's languages to take offline.
 * When closed, the dialog promise will resolve to an array of page ids to be taken offline.
 */
@Component({
    selector: 'gtx-publish-pages-modal',
    templateUrl: './publish-pages-modal.component.html',
    styleUrls: ['./publish-pages-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class PublishPagesModalComponent extends BaseModal<Page[]> implements OnInit {

    @Input()
    public pages: Page[];

    /**
     * The available variants to select.
     */
    @Input()
    public variants: LanguageVariantMap;

    /**
     * If this modal was opened to publish only the current language (`folderLanguage`).
     * When this is `true`, it'll only display pages which do *not* have the `folderLanguage` available,
     * and therefore has to prompt the user what to do with these.
     * Otherwise, simply show them all available variants to select.
     */
    @Input()
    public selectVariants = false;

    /**
     * The language of the folder the user is currently in
     */
    public folderLanguage: Language;

    // Computed values
    pagesWithoutCurrentLanguage: Page[];
    selectedLanguageVariants: { [pageId: number]: number[] } = {};
    selectCount: number;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.folderLanguage = this.entityResolver.getLanguage(this.appState.now.folder.activeLanguage);
        this.pagesWithoutCurrentLanguage = [];

        this.pages.forEach(item => {
            if (item.language !== this.folderLanguage.code) {
                this.pagesWithoutCurrentLanguage.push(item);
            }
        });

        this.resetLanguageVariantSelection();
    }

    confirm(): void {
        const idsToPublish = this.flattenMap(this.selectedLanguageVariants);
        const entities: Page[] = idsToPublish.map(id => this.entityResolver.getEntity('page', id));
        this.closeFn(entities);
    }

    /**
     * Selects all language-variants
     */
    selectAllLanguageVariants(): void {
        // Clear all previously selected ones
        this.selectedLanguageVariants = {};

        for (const [id, variants] of Object.entries(this.variants)) {
            this.selectedLanguageVariants[id] = (variants as Page[]).map(page => page.id);
        }
        this.selectCount = this.flattenMap(this.selectedLanguageVariants).length;

        this.changeDetector.markForCheck();
    }

    /**
     * Resets selection to the default values
     */
    resetLanguageVariantSelection(): void {
        for (const currentPage of this.pages) {
            if (currentPage.language === this.folderLanguage.code && !this.selectVariants) {
                this.selectedLanguageVariants[currentPage.id] = [currentPage.id];
            } else {
                this.selectedLanguageVariants[currentPage.id] = [];
            }
        }
        this.selectCount = this.flattenMap(this.selectedLanguageVariants).length;

        this.changeDetector.markForCheck();
    }

    /**
     * Handles changes to the language variants selection for pages.
     */
    onLanguageSelectionChange(itemId: number, variantIds: number[]): void {
        this.selectedLanguageVariants[itemId] = variantIds;
        this.selectCount = this.flattenMap(this.selectedLanguageVariants).length;

        this.changeDetector.markForCheck();
    }

    /**
     * Given a map of { id: T[] }, flattens it into an array of T.
     */
    private flattenMap<T>(hashMap: { [id: number]: T[] }): T[] {
        return Object.values(hashMap).flatMap(entry => entry);
    }
}
