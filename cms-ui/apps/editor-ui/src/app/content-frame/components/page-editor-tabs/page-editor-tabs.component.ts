import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaToolbarTabsSettings } from '@gentics/aloha-models';
import { NodeFeature } from '@gentics/cms-models';
import { Subscription, combineLatest, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { DefaultEditorControlTabs } from '../../../common/models';
import { AlohaIntegrationService, TAB_ID_LINK_CHECKER } from '../../providers/aloha-integration/aloha-integration.service';
import { OverflowManager } from '../../utils';

@Component({
    selector: 'gtx-page-editor-tabs',
    templateUrl: './page-editor-tabs.component.html',
    styleUrls: ['./page-editor-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorTabsComponent implements OnInit, AfterViewInit, OnDestroy {

    public readonly TAB_ID_LINK_CHECKER = TAB_ID_LINK_CHECKER;

    @Input()
    public brokenLinkCount: number;

    @ViewChild('tabContainer')
    public tabContainerRef: ElementRef<HTMLElement>;

    public activeTab: string;
    public tabs: AlohaToolbarTabsSettings[] = [];
    public tagEditorOpen = false;
    public linkCheckerEnabled = false;

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
            if (!enabled && this.activeTab === TAB_ID_LINK_CHECKER) {
                this.aloha.changeActivePageEditorTab(this.tabs?.[0]?.id);
            }
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.activeToolbarSettings$.subscribe(toolbar => {
            this.tabs = toolbar?.tabs ?? [];
            if (!this.tabs.some(tab => tab.id === this.activeTab)) {
                this.setActiveTab(DefaultEditorControlTabs.FORMATTING);
            }
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.activeEditor$.subscribe(activeEditor => {
            this.activeTab = activeEditor;
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

    public setActiveTab(tab: string): void {
        this.aloha.changeActivePageEditorTab(tab);
    }
}
