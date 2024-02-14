import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, NgZone, OnDestroy, OnInit } from '@angular/core';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaToolbarTabsSettings } from '@gentics/aloha-models';
import { Subscription } from 'rxjs';
import { DefaultEditorControlTabs } from '../../../common/models';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { OverflowManager } from '../../utils';

@Component({
    selector: 'gtx-page-editor-tabs',
    templateUrl: './page-editor-tabs.component.html',
    styleUrls: ['./page-editor-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorTabsComponent implements OnInit, OnDestroy {

    public activeTab: string;
    public tabs: AlohaToolbarTabsSettings[] = [];
    public tagEditorOpen = false;

    protected subscriptions: Subscription[] = [];
    protected overflow: OverflowManager;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected zone: NgZone,
        protected element: ElementRef<HTMLDivElement>,
        protected appState: ApplicationStateService,
        protected aloha: AlohaIntegrationService,
    ) {}

    public ngOnInit(): void {
        this.activeTab = this.aloha.activeTab;

        this.zone.runOutsideAngular(() => {
            this.overflow = new OverflowManager(this.element.nativeElement);
            this.overflow.init();
        });

        this.subscriptions.push(this.appState.select(state => state.ui.tagEditorOpen).subscribe(open => {
            this.tagEditorOpen = open;
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

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public setActiveTab(tab: string): void {
        this.aloha.changeActivePageEditorTab(tab);
    }
}
