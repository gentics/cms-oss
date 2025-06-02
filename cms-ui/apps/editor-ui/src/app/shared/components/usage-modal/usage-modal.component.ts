import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { LINK_TYPES, LinkType, USAGE_TYPES, UsageType } from '@editor-ui/app/common/models';
import { EditMode } from '@gentics/cms-integration-api-models';
import {
    InheritableItem,
    Item,
    Language,
} from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService } from '../../../state';
import { PageLoadEndEvent, PageLoadStartEvent } from '../item-usage-list/item-usage-list.component';

@Component({
    selector: 'gtx-usage-modal',
    templateUrl: './usage-modal.component.html',
    styleUrls: ['./usage-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class UsageModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    public readonly USAGE_TYPES = USAGE_TYPES;
    public readonly LINK_TYPES = LINK_TYPES;

    @Input()
    public item: Item;

    @Input()
    public nodeId: number;

    @Input()
    public currentLanguageId: number;

    public usageCountMap: Record<UsageType, number> = {
        file: 0,
        folder: 0,
        image: 0,
        page: 0,
        template: 0,
        variant: 0,
        tag: 0,
    };
    public usageCount = 0;
    public linkCountMap: Record<LinkType, number> = {
        linkedPage: 0,
        linkedFile: 0,
        linkedImage: 0,
    };
    public linkCount = 0;

    public usageIsLoading = true;
    public linkIsLoading = true;

    public currentNodeId: number;
    public languages: Language[] = [];

    public loadingUsageTypes = new Set<UsageType>();
    public loadingLinkTypes = new Set<LinkType>();
    private subscription: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private navigationService: NavigationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscription.push(this.appState.select(state => state.folder.activeNode).subscribe(nodeId => {
            this.currentNodeId = nodeId;
            this.changeDetector.markForCheck();
        }));

        this.subscription.push(this.appState.select(state => state.entities.language).subscribe(languageMap => {
            this.languages = Object.values(languageMap);
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscription.forEach(s => s.unsubscribe());
    }

    public updateUsageLoad(type: UsageType, event: PageLoadStartEvent): void {
        if (!event.wasLoaded) {
            this.loadingUsageTypes.add(type);
        }
        this.usageIsLoading = this.loadingUsageTypes.size > 0;
    }

    public updateUsageOfType(type: UsageType, event: PageLoadEndEvent): void {
        this.usageCountMap[type] = event.totalCount;
        this.loadingUsageTypes.delete(type);
        this.usageIsLoading = this.loadingUsageTypes.size > 0;
        this.usageCount = Object.values(this.usageCountMap).reduce((acc, val) => acc + val, 0);
        this.changeDetector.markForCheck();
    }

    public updateLinkLoad(type: LinkType, event: PageLoadStartEvent): void {
        if (!event.wasLoaded) {
            this.loadingLinkTypes.add(type);
        }
        this.linkIsLoading = this.loadingLinkTypes.size > 0;
    }

    public updateLinkOfType(type: LinkType, event: PageLoadEndEvent): void {
        this.linkCountMap[type] = event.totalCount;
        this.loadingLinkTypes.delete(type);
        this.linkIsLoading = this.loadingLinkTypes.size > 0;
        this.linkCount = Object.values(this.linkCountMap).reduce((acc, val) => acc + val, 0);
        this.changeDetector.markForCheck();
    }

    /**
     * Handle the item being clicked.
     */
    itemClicked(item: InheritableItem): void {
        const editMode: EditMode = item.type === 'page' ? EditMode.PREVIEW : EditMode.EDIT_PROPERTIES;
        this.navigationService
            .detailOrModal(item.inheritedFromId, item.type, item.id, editMode)
            .navigate();
        this.closeFn(true);
    }
}
