import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaComponent, AlohaLinkChangeEvent, AlohaLinkInsertEvent, AlohaLinkRemoveEvent } from '@gentics/aloha-models';
import { GCNAlohaPlugin, GCNLinkCheckerAlohaPluigin, GCNLinkCheckerPluginSettings } from '@gentics/cms-integration-api-models';
import { ConstructCategory, ExternalLink, NodeFeature, TagType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Subscription, combineLatest, forkJoin, of } from 'rxjs';
import { delay, filter, first, map, switchMap } from 'rxjs/operators';
import { AlohaGlobal } from '../../models/content-frame';
import {
    AlohaIntegrationService,
    NormalizedComponentGroup,
    NormalizedSlotDisplay,
    NormalizedTabsSettings,
    NormalizedToolbarSizeSettings,
    TAB_ID_CONSTRUCTS,
    TAB_ID_LINK_CHECKER,
} from '../../providers/aloha-integration/aloha-integration.service';

const ALOHA_REPO = 'data-gentics-aloha-repository'

function isInternalLink(element: HTMLElement | JQuery): boolean {
    if (element == null) {
        return false;
    }
    if (isJQueryElement(element)) {
        return element.attr(ALOHA_REPO) != null;
    }
    return element.getAttribute(ALOHA_REPO) != null;
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

function isJQueryElement(elem: any): elem is JQuery {
    return elem != null && typeof elem === 'object' && elem.length && typeof elem.attr === 'function';
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
    public initialBrokenLinks: ExternalLink[] = [];
    public brokenLinkElements: HTMLElement[] = [];
    public linkCheckerPlugin: GCNLinkCheckerAlohaPluigin;

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
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(combineLatest([
            this.aloha.reference$.asObservable(),
            this.appState.select(state => state.editor.nodeId).pipe(
                switchMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
                map(features => (features || []).includes(NodeFeature.LINK_CHECKER)),
            ),
        ]).pipe(
            switchMap(([ref, enabled]) => {
                this.linkCheckerEnabled = enabled;
                this.changeDetector.markForCheck();

                if (!ref || !enabled) {
                    return of([false, []]);
                }

                return this.client.linkChecker.pageLinks(this.appState.now.editor.itemId).pipe(
                    map(res => [true, res.items]),
                );
            }),
        ).subscribe(([enabled, links]: [boolean, ExternalLink[]]) => {
            this.initialBrokenLinks = links;
            this.brokenLinkCountChange.emit(this.initialBrokenLinks.filter(link => link.lastStatus === 'invalid').length);
            this.changeDetector.markForCheck();

            if (!enabled) {
                return;
            }

            this.subscriptions.push(forkJoin([
                this.aloha.require('gcnlinkchecker/gcnlinkchecker-plugin').pipe(
                    filter(plugin => plugin != null),
                    first(),
                ),
                // Check if ready, or wait max 10 sec (as the event could have been triggered before)
                combineLatest([
                    this.aloha.bind('aloha-ready'),
                    of(null).pipe(
                        delay(5_000),
                    ),
                ]).pipe(
                    first(),
                ),
            ]).subscribe(([plugin]) => {
                this.linkCheckerPlugin = plugin;
                this.brokenLinkElements = this.linkCheckerPlugin.initializeBrokenLinks(this.initialBrokenLinks).slice();
                this.brokenLinkCountChange.emit(this.linkCheckerPlugin.brokenLinks.length);
                this.changeDetector.markForCheck();

                // Check each link "live"
                if (this.linkCheckerPlugin?.settings?.livecheck) {
                    this.linkCheckerPlugin.uncheckedLinks.forEach(element => this.checkWithDelay(element));
                }
            }));
        }));

        this.subscriptions.push(this.aloha.on<AlohaLinkChangeEvent>('aloha.link.changed').subscribe(event => {
            if (!this.linkCheckerPlugin) {
                return;
            }

            if (event == null || event.element == null || event.element.length === 0) {
                return;
            }

            if (isInternalLink(event.element)) {
                this.linkCheckerPlugin.removeLink(event.element[0]);
            } else if (this.linkCheckerPlugin?.settings?.livecheck) {
                this.checkWithDelay(event.element);
            }
        }));

        this.subscriptions.push(this.aloha.on<AlohaLinkInsertEvent>('aloha.link.insert').subscribe(event => {
            if (!this.linkCheckerPlugin || !this.linkCheckerPlugin?.settings?.livecheck) {
                return;
            }

            if (event == null || event.elements == null || event.elements.length === 0) {
                return;
            }

            Array.from(event.elements).forEach(linkElement => {
                if (isInternalLink(linkElement)) {
                    this.linkCheckerPlugin.removeLink(linkElement);
                } else {
                    this.checkWithDelay(linkElement);
                }
            });
        }));

        this.subscriptions.push(this.aloha.on<AlohaLinkRemoveEvent>('aloha.link.remove').subscribe(() => {
            if (!this.linkCheckerPlugin) {
                return;
            }

            this.linkCheckerPlugin.refreshLinksFromDom();
            this.brokenLinkElements = this.linkCheckerPlugin.brokenLinks.slice();
            this.brokenLinkCountChange.emit(this.brokenLinkElements.length);
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

    public identifyTab(_idx: number, tab: NormalizedTabsSettings): string {
        return tab.id;
    }

    public identifyComponentGroup(_idx: number, group: NormalizedComponentGroup): string {
        return group.slots.map(s => s.name).join(',');
    }

    public identifySlot(_idx: number, slot: NormalizedSlotDisplay): string {
        return slot.name;
    }

    protected checkWithDelay(element: HTMLElement | JQuery, url?: string): void {
        if (isJQueryElement(element)) {
            element = element[0];
        }

        let timerId = parseInt(element.getAttribute(ATTR_QUEUED_LINK_CHECK), 10);
        if (timerId && !Number.isNaN(timerId)) {
            window.clearTimeout(timerId);
        }
        this.linkCheckerPlugin.addUncheckedLink(element);

        if (!url) {
            url = (element.getAttribute('href') || '').trim();
        }

        timerId = window.setTimeout(() => {
            (element as HTMLElement).removeAttribute(ATTR_QUEUED_LINK_CHECK);
            this.checkLink(url, element as HTMLElement);
        }, this.linkCheckerPlugin.settings.delay ?? DEFAULT_DELAY);
        element.setAttribute(ATTR_QUEUED_LINK_CHECK, `${timerId}`);
    }

    protected checkLink(url: string, element: HTMLElement): void {
        this.subscriptions.push(this.client.linkChecker.check({
            url: normalizeURL(url, this.linkCheckerPlugin.settings),
        }).subscribe(res => {
            this.linkCheckerPlugin.updateLinkStatus(element, res);
            this.brokenLinkElements = this.linkCheckerPlugin.brokenLinks.slice();
            this.brokenLinkCountChange.emit(this.linkCheckerPlugin.brokenLinks.length);
            this.changeDetector.markForCheck();
        }));
    }
}
