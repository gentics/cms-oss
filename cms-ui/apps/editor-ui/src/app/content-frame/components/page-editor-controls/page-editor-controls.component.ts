import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { DefaultEditorControlTabs, PageEditorTab } from '@editor-ui/app/common/models';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { ConstructCategory, TagType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Subscription } from 'rxjs';
import { AlohaGlobal } from '../../models/content-frame';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';

@Component({
    selector: 'gtx-page-editor-controls',
    templateUrl: './page-editor-controls.component.html',
    styleUrls: ['./page-editor-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorControlsComponent implements OnInit, OnDestroy {

    public readonly DefaultEditorControlTabs = DefaultEditorControlTabs;

    public activeTab: string;
    public editors: Record<string, PageEditorTab> = {};

    /*
     * Editor Elements which are handled/loaded in this component
     */
    public constructs: TagType[] = [];
    public constructCategories: ConstructCategory[] = [];
    public overlayActive = false;

    /*
     * Aloha data for the individual tabs
     */
    public alohaRef: AlohaGlobal;
    public alohaSettings: AlohaSettings;
    public alohaRange: AlohaRangeObject;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected appState: ApplicationStateService,
        protected api: GcmsApi,
        protected aloha: AlohaIntegrationService,
    ) {}

    ngOnInit(): void {
        this.editors = this.aloha.editors;
        this.activeTab = this.aloha.activeEditor;

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
        }));

        this.subscriptions.push(this.aloha.contextChange$.asObservable().subscribe(range => {
            this.alohaRange = range;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.settings$.asObservable().subscribe(settings => {
            this.alohaSettings = settings;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.activeEditor$.subscribe(active => {
            this.activeTab = active;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.editorsChange$.subscribe(() => {
            this.editors = this.aloha.editors;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.ui.overlayCount).subscribe(count => {
            this.overlayActive = count > 0;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }
}
