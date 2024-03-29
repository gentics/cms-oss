import { Tab, TabGroup } from '@admin-ui/common';
import { SelectState } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    TemplateRef,
} from '@angular/core';
import { Feature, Tag, TagEditorChange } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

type TabItems = (Tab | TabGroup)[];
type PropertiesMap = { [tabId: string]: any };
type TagNameMap = { [id: string]: string };

@Component({
    selector: 'gtx-properties-manager',
    templateUrl: './properties-manager.component.html',
    styleUrls: ['./properties-manager.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PropertiesManagerComponent implements OnChanges, OnInit {

    @Input()
    public nodeId: number;

    @Input()
    public itemType: string;

    @Input()
    public item: any;

    @Input()
    public tabs: TabItems = [];

    @Input()
    public editors: { [tabId: string]: TemplateRef<any> } = {};

    @Input()
    public activeTab: string;

    @Output()
    public change = new EventEmitter<TagEditorChange>();

    @Output()
    public activeTabChange = new EventEmitter<string>();

    @SelectState(state => state.features.global[Feature.TAGFILL_LIGHT])
    public tagfillLightState$: Observable<boolean>;

    baseUrl = new URL(environment.editorUrl, window.location.toString());

    public finalTabs: TabItems = [];
    public propertiesMap: PropertiesMap = {};
    public tagNameMap: TagNameMap;
    public oldTagfillUrl?: string;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.item || changes.tabs) {
            this.rebuildTabs();
        }
    }

    ngOnInit(): void {
        if (!this.activeTab) {
            setTimeout(() => {
                this.setActiveTab(this.finalTabs[0]?.id);
            }, 10);
        }
    }

    setActiveTab(tab: string): void {
        if (this.activeTab === tab) {
            return;
        }

        this.activeTab = tab;
        this.changeDetector.markForCheck();
        this.activeTabChange.emit(tab);
    }

    rebuildTabs(): void {
        const { tabs, properties, tagNames } = this.createTabsFromItem(this.item);
        this.propertiesMap = properties;
        this.finalTabs = [...this.tabs, ...tabs];
        this.tagNameMap = tagNames;
    }

    createTabsFromItem(item: any): { tabs: TabItems; properties: PropertiesMap, tagNames: TagNameMap } {
        const tabs: { [id: string]: (Tab | TabGroup) } = {};
        const properties: PropertiesMap = {};
        const tagNames: TagNameMap = {};

        if (item) {
            if (item.objectTags != null && typeof item.objectTags === 'object') {
                Object.entries<Tag>(item.objectTags).forEach(([tagName, tag]) => {
                    this.saveTagToMap(properties, tagNames, tabs, 'object', tagName, tag);
                });
            }

            if (item.templateTags != null && typeof item.templateTags === 'object') {
                Object.entries<Tag>(item.templateTags).forEach(([tagName, tag]) => {
                    this.saveTagToMap(properties, tagNames, tabs, 'template', tagName, tag);
                });
            }
        }

        return { tabs: Object.values(tabs), properties, tagNames };
    }

    saveTagToMap(
        properties: PropertiesMap,
        tagNames: TagNameMap,
        tabs: { [id: string]: Tab | TabGroup },
        prefix: string,
        tagName: string,
        tag: Tag,
    ): void {
        const tab: Tab = {
            isGroup: false,
            id: `${prefix}_${tagName}`,
            label: tag.name,
        };

        tagNames[tab.id] = tagName;
        if (!tag.construct?.category) {
            properties[tab.id] = tag;
            tabs[tab.id] = tab;
            return;
        }

        const groupId = `${prefix}_category_${this.toSlug(tag.construct.category)}`;
        let group: TabGroup;
        if (tabs[groupId]) {
            group = tabs[groupId] as TabGroup;
        } else {
            group = {
                isGroup: true,
                id: groupId,
                label: tag.construct.category,
                expanded: true,
                tabs: [],
            };
            tabs[groupId] = group;
        }

        group.tabs.push(tab);
        properties[tab.id] = tag;
    }

    forwardTagEditorChange(change: TagEditorChange): void {
        this.change.emit(change);
    }

    toSlug(val: string): string {
        return val.toLowerCase().replace(/[\W]+/g, '-').replace(/-{2,}/g, '-').replace(/^-|-$/g, '');
    }

}
