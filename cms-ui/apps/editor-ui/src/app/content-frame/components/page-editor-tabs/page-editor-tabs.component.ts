import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { TAB_ID_LINK_CHECKER } from '@gentics/cms-integration-api-models';
import { NodeFeature } from '@gentics/cms-models';
import { Subscription, combineLatest, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';
import { AlohaIntegrationService, NormalizedTabsSettings } from '../../providers/aloha-integration/aloha-integration.service';
import { OverflowManager } from '../../utils';

@Component({
    selector: 'gtx-page-editor-tabs',
    templateUrl: './page-editor-tabs.component.html',
    styleUrls: ['./page-editor-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class PageEditorTabsComponent implements OnInit, AfterViewInit, OnDestroy {

    public readonly TAB_ID_LINK_CHECKER = TAB_ID_LINK_CHECKER;

    @Input()
    public brokenLinkCount: number;

    @ViewChild('tabContainer')
    public tabContainerRef: ElementRef<HTMLElement>;

    public activeTab: string;
    public tabs: NormalizedTabsSettings[] = [];
    public visibleTabs: NormalizedTabsSettings[] = [];
    public tagEditorOpen = false;
    public linkCheckerEnabled = false;

    /**
     * When we "force" the user to a different tab, because the initial one wasn't visible anymore,
     * then we save the id in here to recover it.
     * Will be reset once the user manually changes the tab.
     */
    protected forcedOffTabId: string | null = null;

    protected subscriptions: Subscription[] = [];
    protected overflow: OverflowManager;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected zone: NgZone,
        protected appState: ApplicationStateService,
        protected aloha: AlohaIntegrationService,
    ) {}

    public ngOnInit(): void {
        this.activeTab = this.aloha.activeTab;

        this.subscriptions.push(this.appState.select(state => state.ui.tagEditorOpen).subscribe(open => {
            this.tagEditorOpen = open;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.editor.itemType),
            this.appState.select(state => state.editor.nodeId),
        ]).pipe(
            switchMap(([itemType, nodeId]) => {
                if (itemType !== 'page' || !nodeId) {
                    return of(false);
                }

                return this.appState.select(state => state.features.nodeFeatures[nodeId]).pipe(
                    map(features => (features || []).includes(NodeFeature.LINK_CHECKER)),
                );
            }),
        ).subscribe(enabled => {
            this.linkCheckerEnabled = enabled;
            this.updateVisibleTabs();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.activeToolbarSettings$.subscribe(toolbar => {
            this.tabs = toolbar?.tabs ?? [];
            this.updateVisibleTabs();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.editorTab$.subscribe(newTab => {
            // Can't set the tab to a tab which isn't visible
            if (!this.visibleTabs.some(tab => tab.id === newTab)) {
                if (this.visibleTabs.some(tab => tab.id === this.activeTab)) {
                    this.aloha.changeActivePageEditorTab(this.activeTab);
                    return;
                } else if (this.visibleTabs.length > 0) {
                    this.aloha.changeActivePageEditorTab(this.visibleTabs[0].id);
                    return;
                }

                // Edge case where nothing is visible to begin with, so just leave the tab as it is
            }

            this.activeTab = newTab;
            this.changeDetector.markForCheck();
        }))
    }

    public ngAfterViewInit(): void {
        this.zone.runOutsideAngular(() => {
            this.overflow = new OverflowManager(this.tabContainerRef.nativeElement);
            this.overflow.init();
        });
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        if (this.overflow) {
            this.overflow.destroy();
            this.overflow = null;
        }
    }

    public identifyTab(_idx: number, tab: NormalizedTabsSettings): string {
        return tab.id;
    }

    public updateVisibleTabs(): void {
        this.visibleTabs = this.tabs.filter(tab => tab.hasVisibleGroups && tab.id !== TAB_ID_LINK_CHECKER || this.linkCheckerEnabled);

        // In case the active tab is a tab which isn't visible anymore, we update the active tab to the first best one
        if (!this.visibleTabs.some(tab => tab.id === this.activeTab)) {
            this.forcedOffTabId = this.activeTab;
            this.aloha.changeActivePageEditorTab(this.visibleTabs[0]?.id);
        } else if (this.forcedOffTabId != null) {
            this.aloha.changeActivePageEditorTab(this.forcedOffTabId);
            this.forcedOffTabId = null;
        }
    }

    public setActiveTab(tab: string): void {
        this.aloha.changeActivePageEditorTab(tab);
        this.aloha.restoreSelection();
    }

    public handleUserChangeTab(tab: string): void {
        this.forcedOffTabId = null;
        this.setActiveTab(tab);
    }
}
