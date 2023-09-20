import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { ConstructCategory, TagType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Subscription } from 'rxjs';

enum DefaultEditorControlTabs {
    FORMATTING = 'formatting',
    CONSTRUCTS = 'constructs',
    LINK_SETTINGS = 'link-settings',
    TABLE_SETTINGS = 'table-settings',
}

@Component({
    selector: 'gtx-page-editor-controls',
    templateUrl: './page-editor-controls.component.html',
    styleUrls: ['./page-editor-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorControlsComponent implements OnInit, OnDestroy {

    public readonly DefaultEditorControlTabs = DefaultEditorControlTabs;

    public activeTab: DefaultEditorControlTabs | string = DefaultEditorControlTabs.FORMATTING;

    /*
     * Editor Elements which are handled/loaded in this component
     */
    public constructs: TagType[] = [];
    public constructCategories: ConstructCategory[] = [];

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected api: GcmsApi,
    ) {}

    ngOnInit(): void {
        this.subscriptions.push(this.api.tagType.getTagTypes().subscribe(res => {
            this.constructs = res.items;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.api.constructCategory.getConstructCategoryCategories({ recursive: false }).subscribe(res => {
            this.constructCategories = res.items;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public setActiveTab(tab: DefaultEditorControlTabs | string): void {
        this.activeTab = tab;
    }
}
