import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaComponent, GCNAlohaPlugin } from '@gentics/aloha-models';
import { ConstructCategory, TagType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Subscription, combineLatest } from 'rxjs';
import {
    AlohaIntegrationService,
    NormalizedTabsSettings,
    TAB_ID_CONSTRUCTS,
    TAB_ID_LINK_CHECKER,
} from '../../providers/aloha-integration/aloha-integration.service';
import { AlohaGlobal } from '../../models/content-frame';

@Component({
    selector: 'gtx-page-editor-controls',
    templateUrl: './page-editor-controls.component.html',
    styleUrls: ['./page-editor-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorControlsComponent implements OnInit, OnDestroy {

    public readonly TAB_ID_CONSTRUCTS = TAB_ID_CONSTRUCTS;
    public readonly TAB_ID_LINK_CHECKER = TAB_ID_LINK_CHECKER;

    public activeTab: string;
    public settings: NormalizedTabsSettings;
    public components: Record<string, AlohaComponent> = {};
    public gcnPlugin: GCNAlohaPlugin;
    public alohaRef: AlohaGlobal;
    public tagEditorOpen = false;

    /*
     * Editor Elements which are handled/loaded in this component
     */
    public constructs: TagType[] = [];
    public constructCategories: ConstructCategory[] = [];
    public overlayActive = false;
    public constructFavourites: string[] = [];

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected appState: ApplicationStateService,
        protected client: GCMSRestClientService,
        protected aloha: AlohaIntegrationService,
        protected userSettings: UserSettingsService,
    ) {}

    public ngOnInit(): void {
        this.activeTab = this.aloha.activeTab;

        this.subscriptions.push(this.client.construct.list().subscribe(res => {
            this.constructs = res.items;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.client.constructCategory.list({ recursive: false }).subscribe(res => {
            this.constructCategories = res.items;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.ui.overlayCount).subscribe(count => {
            this.overlayActive = count > 0;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.ui.tagEditorOpen).subscribe(open => {
            this.tagEditorOpen = open;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.aloha.activeEditor$,
            this.aloha.activeToolbarSettings$,
        ]).subscribe(([editor, settings]) => {
            this.activeTab = editor;
            this.settings = (settings?.tabs ?? []).find(tab => tab.id === editor);
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.components$.subscribe(components => {
            this.components = components;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.gcnPlugin$.subscribe(gcnPlugin => {
            this.gcnPlugin = gcnPlugin;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.ui.constructFavourites).subscribe(favourites => {
            this.constructFavourites = favourites;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.reference$.subscribe(ref => {
            this.alohaRef = ref;
            this.changeDetector.markForCheck();
        }));
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateFavourites(favourites: string[]): void {
        this.userSettings.setConstructFavourites(favourites);
    }
}
