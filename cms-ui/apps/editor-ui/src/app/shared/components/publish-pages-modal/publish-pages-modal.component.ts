import { Component } from '@angular/core';
import { LanguageVariantMap } from '@editor-ui/app/common/models';
import { ItemType, Language, Page } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService } from '../../../state';

/**
 * A dialog used to indicate which variants of a page's languages to take offline.
 * When closed, the dialog promise will resolve to an array of page ids to be taken offline.
 */
@Component({
    selector: 'publish-pages-modal',
    templateUrl: './publish-pages-modal.tpl.html',
    styleUrls: ['./publish-pages-modal.scss']
})
export class PublishPagesModalComponent implements IModalDialog {

    closeFn: (pagesToPublish: Page[]) => void;
    cancelFn: (val?: any) => void;

    // Should be passed in by the function which creates the modal
    activeLanguage: Language;
    pagesToPublish: Page[];
    pagesToPublishWithCurrentLanguage: Page[];
    pagesToPublishWithoutCurrentLanguage: Page[];
    pageLanguageVariants: LanguageVariantMap;
    publishLanguageVariants: Boolean = false; // default is false for publishing the current language by default

    itemType: ItemType = 'page';
    selectedLanguageVariants: { [pageId: number]: number[] } = {};

    constructor(
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService
    ) {
        this.activeLanguage = this.entityResolver.getLanguage(this.appState.now.folder.activeLanguage);
        this.pagesToPublishWithCurrentLanguage = [];
        this.pagesToPublishWithoutCurrentLanguage = [];
    }

    get selectCount(): number {
        return this.flattenMap(this.selectedLanguageVariants).length;
    }

    ngOnInit(): void {
        this.pagesToPublish.forEach( item => {
            if ( item.language == this.activeLanguage.code ) {
                this.pagesToPublishWithCurrentLanguage.push(item);
            } else {
                this.pagesToPublishWithoutCurrentLanguage.push(item);
            }
        });
        this.resetLanguageVariantSelection();
    }

    confirm(): void {
        const idsToPublish = this.flattenMap(this.selectedLanguageVariants);
        const entities: Page[] = idsToPublish.map( id => this.entityResolver.getEntity('page', id) );
        this.closeFn(entities);
    }

    /**
     * Selects every language variants
     */
    selectAllLanguageVariant(): void {
        this.pagesToPublish.forEach( item => {
            this.selectedLanguageVariants[item.id] = [...(<any> Object).values(item.languageVariants)];
        });
    }

    /**
     * Resets selection to the default values
     */
    resetLanguageVariantSelection(): void {
        this.pagesToPublish.forEach( item => {
            if ( item.language == this.activeLanguage.code && !this.publishLanguageVariants ) {
                this.selectedLanguageVariants[item.id] = [item.id];
            } else {
                this.selectedLanguageVariants[item.id] = [];
            }
        });
    }

    /**
     * Handles changes to the language variants selection for pages.
     */
    onLanguageSelectionChange(itemId: number, variantIds: number[], checkLocalizations: boolean = false): void {
        this.selectedLanguageVariants[itemId] = variantIds;
    }

    registerCloseFn(close: (pagesToPublish: Page[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * Given a map of { id: T[] }, flattens it into an array of T.
     */
    private flattenMap<T>(hashMap: { [id: number]: T[] }): T[] {
        return Object.keys(hashMap).reduce((all, id) => all.concat(hashMap[+id]), []);
    }
}
