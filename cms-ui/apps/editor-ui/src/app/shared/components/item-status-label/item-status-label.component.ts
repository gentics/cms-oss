import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { UIMode } from '@editor-ui/app/common/models';
import { Form, Language, Page, StagedItemsMap } from '@gentics/cms-models';
import { PublishableStateUtil } from '../../util/entity-states';

@Component({
    selector: 'item-status-label',
    templateUrl: './item-status-label.component.html',
    styleUrls: ['./item-status-label.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemStatusLabelComponent {

    readonly UIMode = UIMode;

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

    labelTextStateInQueue(): boolean {
        // NOTE: It is specified that only queued ```publishAt``` requests indicate this state
        if (!this.item || !this.item.timeManagement) { return; }
        return this.item.timeManagement.queuedPublish ? true : false;
    }

    labelTextStatePlannedForPublishing(): boolean {
        if (!this.item) { return; }
        return (
            PublishableStateUtil.statePlanned(this.item)
            && PublishableStateUtil.statePlannedOnline(this.item)
            && !this.labelTextStateInQueue()
            && !this.labelTextStateDeleted()
        );
    }

    labelTextStatePlannedForTakingOffline(): boolean {
        if (!this.item) { return; }
        return (
            PublishableStateUtil.statePlanned(this.item)
            && PublishableStateUtil.statePlannedOffline(this.item)
            && !PublishableStateUtil.statePlannedOnline(this.item)
            && !this.labelTextStateInQueue()
            && !this.labelTextStateDeleted()
        );
    }

    labelTextStateModified(): boolean {
        if (!this.item) { return; }
        return (
            PublishableStateUtil.stateModified(this.item)
            && !PublishableStateUtil.statePlanned(this.item)
            && !this.labelTextStateInQueue()
            && !this.labelTextStateDeleted()
        );
    }

    stateModified(): boolean {
        return PublishableStateUtil.stateModified(this.item);
    }

    labelTextStatePublished(): boolean {
        if (!this.item) { return; }
        return (
            PublishableStateUtil.statePublished(this.item)
            && !PublishableStateUtil.stateModified(this.item)
            && !PublishableStateUtil.statePlanned(this.item)
            && !this.labelTextStateInQueue()
            && !this.labelTextStateDeleted()
        );
    }

    labelTextStateOffline(): boolean {
        if (!this.item) { return; }
        return (
            PublishableStateUtil.stateOffline(this.item)
            && !PublishableStateUtil.stateModified(this.item)
            && !PublishableStateUtil.statePlanned(this.item)
            && !this.labelTextStateInQueue()
            && !this.labelTextStateDeleted()

        );
    }

    labelTextStateLocked(): boolean {
        if (!this.item) { return; }
        return (
            PublishableStateUtil.stateLocked(this.item)
            && !PublishableStateUtil.stateModified(this.item)
            && !PublishableStateUtil.statePlanned(this.item)
            && !this.labelTextStateInQueue()
            && !this.labelTextStateDeleted()
        );
    }

    labelTextStateDeleted(): boolean {
        if (!this.item) { return; }
        return PublishableStateUtil.stateDeleted(this.item);
    }
}
