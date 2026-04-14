import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { I18nNotificationService, I18nService } from '@gentics/cms-components';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Form, Item, Language } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { FolderActionsService } from '../../../state';
import { EntityStateUtil } from '../../util/entity-states';
import { BaseItemListHeaderComponent } from '../base-item-list-header/base-item-list-header.component';
import { CreateFormModalComponent } from '../create-form-modal/create-form-modal.component';
import { DisplayFieldSelectorModal } from '../display-field-selector/display-field-selector.component';
import { SortingModal } from '../sorting-modal/sorting-modal.component';

@Component({
    selector: 'gtx-form-list-header',
    templateUrl: './form-list-header.component.html',
    styleUrls: ['./form-list-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormListHeaderComponent extends BaseItemListHeaderComponent {

    /** If this header is for external forms */
    public readonly external = input.required<boolean>();
    /** All languages the current node has */
    public readonly nodeLanguages = input.required<Language[]>();
    public readonly displayFields = input<string[]>();

    /** Emits an event when this header performed an action where the list has to reload. */
    public readonly requestReload = output<void>();

    private modalService = inject(ModalService);
    private navigationService = inject(NavigationService);
    private errorHandler = inject(ErrorHandler);
    private userSettings = inject(UserSettingsService);
    private folderActions = inject(FolderActionsService);
    private notifications = inject(I18nNotificationService);
    private i18n = inject(I18nService);
    private contextMenuOperations = inject(ContextMenuOperationsService);

    public createClicked(): void {
        this.modalService.fromComponent(
            CreateFormModalComponent,
            { width: '600px' },
            {
                languages: this.nodeLanguages(),
            },
        )
            .then((modal) => modal.open())
            .then((newItem: Form) => {
                this.requestReload.emit();
                this.navigationService.detailOrModal(this.node().id, 'form', newItem.id, EditMode.EDIT).navigate();
            })
            .catch(this.errorHandler.catch);
    }

    /**
     * Open the DisplayFieldSelector component in a modal.
     */
    selectDisplayFields(): void {
        const fields = this.displayFields().slice(0);
        this.modalService.fromComponent(DisplayFieldSelectorModal, {}, { type: 'form', fields })
            .then((modal) => modal.open())
            .then((result: { selection: string[]; showPath: boolean }) => {
                this.userSettings.setDisplayFields('form', result.selection);
            })
            .catch(this.errorHandler.catch);
    }

    /**
     * Open the modal for selecting sort option.
     */
    selectSorting(): void {
        const locals: Partial<SortingModal> = {
            itemType: 'form',
            sortBy: this.sortBy() as any,
            sortOrder: this.sortOrder() as any,
        };

        this.modalService.fromComponent(SortingModal, {}, locals)
            .then((modal) => modal.open())
            .then((sorting) => {
                this.userSettings.setSorting('form', sorting.sortBy, sorting.sortOrder);
            })
            .catch(this.errorHandler.catch);
    }

    toggleDisplayAllLanguages(): void {
        this.userSettings.setDisplayAllLanguages(!this.showAllLanguages());
    }

    toggleDisplayStatusIcons(): void {
        this.userSettings.setDisplayStatusIcons(!this.showStatusIcons());
    }

    toggleDisplayDeleted(): void {
        this.userSettings.setDisplayDeleted(!this.showDeleted());
        // refresh list as refetch is required
        this.folderActions.refreshList('folder');
        this.folderActions.refreshList('page');
        this.folderActions.refreshList('file');
        this.folderActions.refreshList('image');
    }

    /**
     * Copy the selected items to a different folder and clear the selection.
     */
    copySelected(): void {
        const itemsToCopy = this.getNotDeletedItems();
        if (itemsToCopy.length === 0) {
            return;
        }

        this.contextMenuOperations.copyItems('form', itemsToCopy, this.node().id).then((folder) => {
            if (folder) {
                this.toggleAllSelection.emit(false);
                this.requestReload.emit();
            }
        });
    }

    /**
     * Move the selected items to a different folder and clear the selection.
     */
    moveSelected(): void {
        const itemsToMove = this.getNotDeletedItems();
        if (itemsToMove.length === 0) {
            return;
        }

        this.contextMenuOperations.moveItems('form', itemsToMove, this.node().id, this.folderId()).then((success) => {
            if (success) {
                this.toggleAllSelection.emit(false);
                this.requestReload.emit();
            }
        });
    }

    /**
     * Helper method to filter out deleted items and which shows a notification if
     * no regular/not deleted item has been selected yet.
     */
    private getNotDeletedItems(): Item[] {
        const validItems = this.selectedItems().filter((item) => !EntityStateUtil.stateDeleted(item));
        if (validItems.length !== 0) {
            return validItems;
        }

        this.notifications.show({
            message: 'editor.select_not_deleted_items',
            translationParams: {
                itemTypePlural: this.i18n.instant(`common.type_${this.external() ? 'external_' : ''}forms`),
            },
            type: 'warning',
        });
        return [];

    }
}
