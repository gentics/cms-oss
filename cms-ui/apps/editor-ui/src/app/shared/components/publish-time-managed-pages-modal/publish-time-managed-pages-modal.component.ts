import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { ItemType, Page } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { ApplicationStateService, CloseEditorAction, FolderActionsService } from '../../../state';

@Component({
    selector: 'publish-time-managed-pages-modal',
    templateUrl: './publish-time-managed-pages-modal.tpl.html',
    styleUrls: ['./publish-time-managed-pages-modal.scss']
    })

export class PublishTimeManagedPagesModal implements IModalDialog {
    @Input() pages: Page[];
    @Input() allPages: number;
    @Input() closeEditor = true;

    iconForItemType = iconForItemType;
    itemType: ItemType = 'page';

    publishAtChecked = true;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private state: ApplicationStateService,
    ) { }

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

    onRadioButtonsChange(): void {
        // This seems to be necessary to make the radio buttons react correctly.
        this.changeDetector.detectChanges();
    }

    closeFn = (pages: Page[]) => {};

    cancelFn = () => {};

    registerCloseFn(close: (pages: Page[]) => void): void {
        this.closeFn = (pages: Page[]) => {
            // refresh list
            this.folderActions.refreshList('page');
            if (this.closeEditor) {
                this.closeEditorIfPageOpen(pages);
            }
            close(pages);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * In any case desired UX behavior after completing modal actions is closing
     * the content editor if it displays the referred page.
     *
     * @param pages involved in modal actions
     */
    protected closeEditorIfPageOpen(pages: Page[]): void {
        const editorIsOpen = this.state.now.editor.editorIsOpen;
        const currentPageIdInContentFrame = this.state.now.editor.itemId;
        // if content frame is open and if page in content frame is current page
        if (editorIsOpen && pages.find(page => page.id === currentPageIdInContentFrame)) {
            // then close content frame
            this.state.dispatch(new CloseEditorAction());
        }
    }
}
