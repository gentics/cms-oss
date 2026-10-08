import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { Page } from '@gentics/cms-models';
import { BaseModal, TableColumn, TableRow } from '@gentics/ui-core';
import { ApplicationStateService, CloseEditorAction, FolderActionsService } from '../../../state';

@Component({
    selector: 'gtx-publish-time-managed-pages-modal',
    templateUrl: './publish-time-managed-pages-modal.component.html',
    styleUrls: ['./publish-time-managed-pages-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})

export class PublishTimeManagedPagesModal extends BaseModal<Page[]> implements OnInit {

    @Input()
    pages: Page[];

    @Input()
    allPages: number;

    @Input()
    closeEditor = true;

    public rows: TableRow<Page>[] = [];
    public columns: TableColumn<Page>[] = [];

    publishAtChecked = true;

    constructor(
        private folderActions: FolderActionsService,
        private state: ApplicationStateService,
        private i18n: I18nService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.rows = this.pages.map((page) => {
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
                id: 'language',
                label: this.i18n.instant('common.language'),
                fieldPath: 'languageName',
            },
            {
                id: 'publishSchedule',
                label: this.i18n.instant('modal.publish_at_date'),
                mapper: (page: Page) => page.timeManagement?.at || page.timeManagement?.queuedPublish?.at,
            },
        ];
    }

    okayClicked(): void {
        if (this.publishAtChecked) {
            // no changes made
            this.folderActions.publishPagesAt(
                this.pages,
                0,
                true,
                false,
            ).then(() => {
                // refresh list to display changes made
                this.folderActions.refreshList('page');
                return this.closeFn(this.pages);
            });
        } else {
            this.folderActions.publishPages(this.pages)
                .then(() => {
                    // refresh list to display changes made
                    this.folderActions.refreshList('page');
                    return this.closeFn(this.pages);
                });
        }
    }

    /**
     * In any case desired UX behavior after completing modal actions is closing
     * the content editor if it displays the referred page.
     * @param pages involved in modal actions
     */
    protected closeEditorIfPageOpen(pages: Page[]): void {
        const editorIsOpen = this.state.now.editor.editorIsOpen;
        const currentPageIdInContentFrame = this.state.now.editor.itemId;
        // if content frame is open and if page in content frame is current page
        if (editorIsOpen && pages.find((page) => page.id === currentPageIdInContentFrame)) {
            // then close content frame
            this.state.dispatch(new CloseEditorAction());
        }
    }
}
