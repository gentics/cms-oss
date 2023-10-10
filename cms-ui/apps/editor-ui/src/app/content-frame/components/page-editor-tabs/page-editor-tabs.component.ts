import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { DefaultEditorControlTabs, PageEditorTab } from '../../../common/models';

@Component({
    selector: 'gtx-page-editor-tabs',
    templateUrl: './page-editor-tabs.component.html',
    styleUrls: ['./page-editor-tabs.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorTabsComponent implements OnInit, OnDestroy {

    public readonly DefaultEditorControlTabs = DefaultEditorControlTabs;

    public activeTab: string;
    public editors: Record<string, PageEditorTab> = {};

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected aloha: AlohaIntegrationService,
    ) {}

    ngOnInit(): void {
        this.editors = this.aloha.editors;
        this.activeTab = this.aloha.activeEditor;

        this.subscriptions.push(this.aloha.activeEditor$.subscribe(active => {
            if (active == null) {
                this.aloha.changeActivePageEditorTab(DefaultEditorControlTabs.FORMATTING);
                return;
            }
            this.activeTab = active;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.editorsChange$.subscribe(() => {
            this.editors = this.aloha.editors;
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
        this.aloha.changeActivePageEditorTab(tab);
    }
}
