import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { InheritableItem, Page } from '@gentics/cms-models';
import { BaseModal, TableColumn, TableRow } from '@gentics/ui-core';
import { LanguageVariantMap } from '../../../common/models';
import { I18nService } from '@gentics/cms-components';

export interface MultiPagesOfflineResult {
    delete: InheritableItem[];
    unlocalize: InheritableItem[];
}

/**
 * A dialog used to indicate which variants of a page's languages to take offline.
 * When closed, the dialog promise will resolve to an array of page ids to be taken offline.
 */
@Component({
    selector: 'gtx-take-pages-offline-modal',
    templateUrl: './take-pages-offline-modal.component.html',
    styleUrls: ['./take-pages-offline-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class TakePagesOfflineModal extends BaseModal<number[]> implements OnInit {

    @Input()
    pagesToTakeOffline: Page[];

    @Input()
    pageLanguageVariants: LanguageVariantMap;

    public rows: TableRow<Page>[] = [];
    public columns: TableColumn<Page>[] = [];

    public selectedLanguageVariants: { [pageId: number]: number[] } = {};
    public finalIds: number[] = [];

    constructor(
        private i18n: I18nService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.rows = this.pagesToTakeOffline.map((page) => {
            return {
                id: `${page.id}`,
                item: page,
            };
        });

        this.columns = [
            {
                id: 'name',
                label: this.i18n.instant('common.name'),
                fieldPath: 'name',
            },
            {
                id: 'languages',
                label: this.i18n.instant('editor.item_page_language_variant_plural'),
            },
        ];

        this.pagesToTakeOffline.forEach((item) => {
            this.selectedLanguageVariants[item.id] = [item.id];
        });
    }

    /**
     * Handles changes to the language variants selection for pages.
     */
    onLanguageSelectionChange(itemId: number, variantIds: number[]): void {
        this.selectedLanguageVariants[itemId] = variantIds;
        this.finalIds = Object.values(this.selectedLanguageVariants).flatMap((ids) => ids);
    }
}
