import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Tab, TabGroup } from '../../../common';

@Component({
    selector: 'gtx-properties-tabs',
    templateUrl: './properties-tabs.component.html',
    styleUrls: ['./properties-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class PropertiesTabsComponent implements OnChanges {

    @Input()
    public items: (Tab | TabGroup)[] = [];

    @Input()
    public selectedTab?: string;

    @Output()
    public selectedTabChange = new EventEmitter<string>();

    @Output()
    public groupToggle = new EventEmitter<{ id: string, expanded: boolean }>();

    public itemsMap: { [id: string]: Tab | TabGroup } = {};

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.items) {
            this.updateItemsMap();
        }
    }

    updateItemsMap(): void {
        this.itemsMap = {};
        this.items.forEach(item => {
            this.itemsMap[item.id] = item;
            if (item.isGroup) {
                item.tabs.forEach(tab => {
                    this.itemsMap[tab.id] = tab;
                });
            }
        });
    }

    selectTab(tabId: string): void {
        this.selectedTab = tabId;
        this.selectedTabChange.emit(tabId);
    }

    toggleGroup({ tabId, expand }: { tabId: string, expand: boolean }): void {
        const item = this.itemsMap[tabId];
        if (item != null && item.isGroup) {
            item.expanded = expand;
            this.groupToggle.emit({ id: tabId, expanded: expand });
        }
    }
}
