import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { Form, Language, Page, StagedItemsMap } from '@gentics/cms-models';
import { ChangesOf } from '@gentics/ui-core';
import { ItemState, UIMode } from '../../../common/models';
import { PublishableStateUtil } from '../../util/entity-states';

enum DisplayLabel {
    DELETED,
    IN_QUEUE,
    PLANNED_ONLINE,
    PLANNED_OFFLINE,
    MODIFIED,
    PUBLISHED,
    OFFLINE,
    LOCKED,
}

@Component({
    selector: 'item-status-label',
    templateUrl: './item-status-label.component.html',
    styleUrls: ['./item-status-label.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ItemStatusLabelComponent implements OnChanges {

    public readonly UIMode = UIMode;
    public readonly DisplayLabel = DisplayLabel;

    @Input()
    public item: Page | Form;

    @Input()
    public activeNodeId: number;

    @Input()
    public nodeLanguages: Language[];

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    @Input()
    public iconOnly = false;

    public state: ItemState;
    public labelToDisplay: DisplayLabel;

    public ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.item) {
            this.updateState();
            this.labelToDisplay = this.determineLabelToDisplay();
        }
    }

    private updateState(): void {
        this.state = {
            inQueue: !!(this.item?.timeManagement?.queuedPublish ?? null),
            planned: this.item != null && PublishableStateUtil.statePlanned(this.item),
            plannedOnline: this.item != null && PublishableStateUtil.statePlannedOnline(this.item),
            plannedOffline: this.item != null && PublishableStateUtil.statePlannedOffline(this.item),
            modified: this.item != null && PublishableStateUtil.stateModified(this.item),
            published: this.item != null && PublishableStateUtil.statePublished(this.item),
            offline: this.item != null && PublishableStateUtil.stateOffline(this.item),
            locked: this.item != null && PublishableStateUtil.stateLocked(this.item),
            deleted: this.item != null && PublishableStateUtil.stateDeleted(this.item),
            inherited: this.item != null && PublishableStateUtil.stateInherited(this.item),
            localized: this.item != null && PublishableStateUtil.stateLocalized(this.item),
        };
    }

    private determineLabelToDisplay(): DisplayLabel {
        if (this.state.deleted) {
            return DisplayLabel.DELETED;
        } else if (this.state.inQueue) {
            return DisplayLabel.IN_QUEUE;
        } else if (this.state.planned) {
            return this.state.plannedOnline ? DisplayLabel.PLANNED_ONLINE : DisplayLabel.PLANNED_OFFLINE;
        } else if (this.state.modified) {
            return DisplayLabel.MODIFIED;
        } else if (this.state.published) {
            return DisplayLabel.PUBLISHED;
        } else if (this.state.offline) {
            return DisplayLabel.OFFLINE;
        } else if (this.state.locked) {
            return DisplayLabel.LOCKED;
        }
    }
}
