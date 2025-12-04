import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Form, Language, Page, StagedItemsMap } from '@gentics/cms-models';
import { UIMode } from '../../../common/models';
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
    standalone: false
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

    public inQueue = false;
    public planned = false;
    public plannedOnline = false;
    public plannedOffline = false;
    public modified = false;
    public published = false;
    public offline = false;
    public locked = false;
    public deleted = false;
    public inherited = false;
    public localized = false;

    public labelToDisplay: DisplayLabel;

    public ngOnChanges(changes: SimpleChanges): void {
        if (!this.item) {
            this.inQueue = false;
            this.planned = false;
            this.plannedOnline = false;
            this.plannedOffline = false;
            this.modified = false;
            this.published = false;
            this.offline = false;
            this.locked = false;
            this.deleted = false;
            this.inherited = false;
            this.localized = false;
        }

        this.inQueue = !!this.item.timeManagement?.queuedPublish;
        this.planned = PublishableStateUtil.statePlanned(this.item);
        this.plannedOnline = PublishableStateUtil.statePlannedOnline(this.item);
        this.plannedOffline = PublishableStateUtil.statePlannedOffline(this.item);
        this.modified = PublishableStateUtil.stateModified(this.item);
        this.published = PublishableStateUtil.statePublished(this.item);
        this.offline = PublishableStateUtil.stateOffline(this.item);
        this.locked = PublishableStateUtil.stateLocked(this.item);
        this.deleted = PublishableStateUtil.stateDeleted(this.item);
        this.inherited = PublishableStateUtil.stateInherited(this.item);
        this.localized = PublishableStateUtil.stateLocalized(this.item);

        this.labelToDisplay = this.determineLabelToDisplay();
    }

    private determineLabelToDisplay(): DisplayLabel {
        if (this.deleted) {
            return DisplayLabel.DELETED;
        } else if (this.inQueue) {
            return DisplayLabel.IN_QUEUE;
        } else if (this.planned) {
            return this.plannedOnline ? DisplayLabel.PLANNED_ONLINE : DisplayLabel.PLANNED_OFFLINE;
        } else if (this.modified) {
            return DisplayLabel.MODIFIED;
        } else if (this.published) {
            return DisplayLabel.PUBLISHED;
        } else if (this.offline) {
            return DisplayLabel.OFFLINE;
        } else if (this.locked) {
            return DisplayLabel.LOCKED;
        }
    }
}
