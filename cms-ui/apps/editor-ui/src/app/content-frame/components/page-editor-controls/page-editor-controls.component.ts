import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, NgZone, OnDestroy, OnInit, Output } from '@angular/core';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaComponent, AlohaLinkChangeEvent, AlohaLinkRemoveEvent, AlohaPubSub } from '@gentics/aloha-models';
import { GCNAlohaPlugin, GCNLinkCheckerAlohaPluigin, GCNLinkCheckerPluginSettings } from '@gentics/cms-integration-api-models';
import { ConstructCategory, NodeFeature, TagType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Subscription, combineLatest, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { AlohaGlobal } from '../../models/content-frame';
import {
    AlohaIntegrationService,
    NormalizedToolbarSizeSettings,
    TAB_ID_CONSTRUCTS,
    TAB_ID_LINK_CHECKER,
} from '../../providers/aloha-integration/aloha-integration.service';

function isInternalLink(element: JQuery): boolean {
    return element != null && element.attr('data-gentics-aloha-repository') != null;
}

function normalizeURL(url: string, settings: GCNLinkCheckerPluginSettings): string {
    if (url.indexOf('//') === 0) {
        return `${settings.defaultProtocol}:${url}`;
    }

    if (url.indexOf('/') === 0) {
        if (typeof settings.absoluteBase === 'string') {
            return `${settings.absoluteBase}${url}`;
        } else {
            return url;
        }
    }

    if (url.indexOf('http') !== 0 && url.indexOf(':') < 0) {
        if (typeof settings.relativeBase === 'string') {
            return `${settings.relativeBase}${url}`;
        } else {
            return url;
        }
    }

    return url;
}

const ATTR_QUEUED_LINK_CHECK = 'gcmsui-queued-link-check';
const DEFAULT_DELAY = 500;

@Component({
    selector: 'gtx-page-editor-controls',
    templateUrl: './page-editor-controls.component.html',
    styleUrls: ['./page-editor-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorControlsComponent implements OnInit, OnDestroy {

    public readonly TAB_ID_CONSTRUCTS = TAB_ID_CONSTRUCTS;
    public readonly TAB_ID_LINK_CHECKER = TAB_ID_LINK_CHECKER;

    @Output()
    public brokenLinkCountChange = new EventEmitter<number>();

    public activeTab: string;
    public settings: NormalizedToolbarSizeSettings;
    public components: Record<string, AlohaComponent> = {};
    public gcnPlugin: GCNAlohaPlugin;
    public alohaRef: AlohaGlobal;
    public tagEditorOpen = false;
    public linkCheckerEnabled = false;

    /*
     * Editor Elements which are handled/loaded in this component
     */
    public constructs: TagType[] = [];
    public constructCategories: ConstructCategory[] = [];
    public overlayActive = false;
    public constructFavourites: string[] = [];

    /* Link Checker properties */
    public intiialBrokenLinks = [];
    public brokenLinkElements: HTMLElement[] = [];
    public linkCheckerPlugin: GCNLinkCheckerAlohaPluigin;
    protected pubSub: AlohaPubSub;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected zone: NgZone,
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

        this.subscriptions.push(this.aloha.activeEditor$.subscribe(activeTab => {
            this.activeTab = activeTab;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.aloha.activeToolbarSettings$.subscribe(settings => {
            this.settings = settings;
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
            this.setupLinkChecker();
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.aloha.reference$,
            this.appState.select(state => state.editor.nodeId).pipe(
                switchMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
                map(features => (features || []).includes(NodeFeature.LINK_CHECKER)),
            ),
        ]).pipe(
            switchMap(([ref, enabled]) => {
                this.linkCheckerEnabled = enabled;
                this.changeDetector.markForCheck();

                if (!ref || !enabled) {
                    return of([]);
                }

                return this.client.linkChecker.pageLinks(this.appState.now.editor.itemId).pipe(
                    map(res => res.items),
                );
            }),
        ).subscribe(links => {
            this.intiialBrokenLinks = links;
            this.setupLinkCheckerPlugin();
            this.brokenLinkCountChange.emit(this.intiialBrokenLinks.length);
            this.changeDetector.markForCheck();
        }));
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateFavourites(favourites: string[]): void {
        this.userSettings.setConstructFavourites(favourites);
    }

    public updateBrokenLinkCount(): void {
        this.brokenLinkCountChange.emit(this.linkCheckerPlugin.brokenLinks.length);
    }

    protected setupLinkChecker(): void {
        if (this.alohaRef == null) {
            this.linkCheckerPlugin = null;
            this.pubSub = null;
            return;
        }

        try {
            this.linkCheckerPlugin = this.alohaRef.require('gcnlinkchecker/gcnlinkchecker-plugin');
            this.setupLinkCheckerPlugin();
        } catch (err) {}

        try {
            this.pubSub = this.alohaRef.require('PubSub');
            this.setupLinkCheckerSubscriptions();
        } catch (err) {}
    }

    protected setupLinkCheckerPlugin(): void {
        if (!this.linkCheckerPlugin) {
            return;
        }

        this.alohaRef.bind('aloha-ready', () => {
            this.brokenLinkElements = this.linkCheckerPlugin.initializeBrokenLinks(this.intiialBrokenLinks).slice();
            this.brokenLinkCountChange.emit(this.linkCheckerPlugin.brokenLinks.length);
        });
    }

    protected setupLinkCheckerSubscriptions(): void {
        // Add an event handler for changed links.
        this.pubSub.sub('aloha.link.changed', (msg: AlohaLinkChangeEvent) => {
            // if livecheck is activated and the link is external and not empty and not only an anchor, the check is done after a delay
            if (
                this.linkCheckerPlugin?.settings?.livecheck
                && msg.href !== ''
                && msg.href.indexOf('#') !== 0
                && !isInternalLink(msg.element)
            ) {
                this.checkWithDelay(msg.href.trim(), msg.element);
            }
        });

        this.pubSub.sub('aloha.link.remove', (_event: AlohaLinkRemoveEvent) => {
            this.linkCheckerPlugin.refreshLinksFromDom();
            this.brokenLinkElements = this.linkCheckerPlugin.brokenLinks.slice();
            this.brokenLinkCountChange.emit(this.brokenLinkElements.length);
        });
    }

    protected checkWithDelay(url: string, element: JQuery): void {
        let timerId = parseInt(element.attr(ATTR_QUEUED_LINK_CHECK), 10);
        if (timerId && !Number.isNaN(timerId)) {
            window.clearTimeout(timerId);
        }

        timerId = window.setTimeout(() => {
            element.removeAttr(ATTR_QUEUED_LINK_CHECK);
            this.checkLink(url, element[0]);
        }, this.linkCheckerPlugin.settings.delay ?? DEFAULT_DELAY);
        element.attr(ATTR_QUEUED_LINK_CHECK, `${timerId}`);
    }

    protected checkLink(url: string, element: HTMLElement): void {
        this.subscriptions.push(this.client.linkChecker.check({
            url: normalizeURL(url, this.linkCheckerPlugin.settings),
        }).subscribe(res => {
            this.zone.runOutsideAngular(() => {
                if (res.valid) {
                    this.linkCheckerPlugin.removeBrokenLink(element);
                } else {
                    this.linkCheckerPlugin.addBrokenLink(element)
                }
                this.brokenLinkElements = this.linkCheckerPlugin.brokenLinks.slice();
                this.brokenLinkCountChange.emit(this.linkCheckerPlugin.brokenLinks.length);
                this.changeDetector.markForCheck();
            });
        }));
    }
}
