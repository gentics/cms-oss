import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ApplicationStateService, ChangeListSelectionAction, FocusEditorAction } from '@editor-ui/app/state';
import { FolderItemType, Image, Item, Node as NodeModel, StagedItemsMap } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { ItemsInfo, UIMode } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { UsageModalComponent } from '../../../shared/components/usage-modal/usage-modal.component';
import { EntityStateUtil } from '../../../shared/util/entity-states';
import { } from '../../../state';

@Component({
    selector: 'grid-item',
    templateUrl: './grid-item.component.html',
    styleUrls: ['./grid-item.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GridItemComponent {

    readonly UIMode = UIMode;

    @Input()
    public item: Image;

    @Input()
    public nodeId: number;

    @Input()
    public selected: boolean;

    @Input()
    public routerLink: string;

    @Input()
    public linkPaths: string;

    @Input()
    public itemsInfo: ItemsInfo;

    @Input()
    public itemType: FolderItemType;

    @Input()
    public activeNode: NodeModel;

    @Input()
    public icon: string;

    @Input()
    public filterTerm: string;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    constructor(
        private appState: ApplicationStateService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
    ) {}

    toggleSelect(): void {
        this.appState.dispatch(new ChangeListSelectionAction(this.itemType, !this.selected ? 'append' : 'remove', [this.item.id]));
        this.selected = !this.selected;
    }

    /**
     * Fire the itemClick event with the item object as the payload.
     */
    itemClicked(e: MouseEvent, item: Item): void {
        e.preventDefault();
        if (item.type === 'page' || item.type === 'file' || item.type === 'image') {
            this.appState.dispatch(new FocusEditorAction());
        }
    }

    /**
     * Opens up a modal displaying the usage for the selected item.
     */
    showUsage(item: Item): void {
        const nodeId = this.activeNode.id;
        const currentLanguageId = this.appState.now.folder.activeLanguage;
        this.modalService.fromComponent(UsageModalComponent, {}, { item, nodeId, currentLanguageId })
            .then(modal => modal.open())
            .catch(this.errorHandler.catch);
    }

    /**
     * @returns TRUE if item has been deleted and is in wastebin
     */
    isDeleted(): boolean {
        if (this.item) {
            return EntityStateUtil.stateDeleted(this.item);
        }
    }

}
