import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { ConstructCategory, TagType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Subscription } from 'rxjs';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { AlohaGlobal } from '../content-frame/common';

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

    /*
     * Aloha data for the individual tabs
     */
    public alohaRef: AlohaGlobal;
    public alohaSettings: AlohaSettings;
    public alohaRange: AlohaRangeObject;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected api: GcmsApi,
        protected aloha: AlohaIntegrationService,
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

        this.subscriptions.push(this.aloha.reference$.asObservable().subscribe(ref => {
            this.alohaRef = ref;
            this.changeDetector.markForCheck();
        }))

        this.subscriptions.push(this.aloha.contextChange$.asObservable().subscribe(range => {
            this.alohaRange = range;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.settings$.asObservable().subscribe(settings => {
            this.alohaSettings = settings;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public setActiveTab(tab: DefaultEditorControlTabs | string | Event): void {
        // This handles the "select" event, which is also triggered by the browser events when ... selecting things.
        // Preventing these browser events from actually changing/breaking the tabs is important here.
        if (tab == null || tab instanceof Event) {
            return;
        }
        this.activeTab = tab;
    }
}
