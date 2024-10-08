import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    OnDestroy,
    OnInit,
    Output,
    QueryList,
    ViewChildren,
} from '@angular/core';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { AlohaComponent, AlohaEditable, AlohaLinkChangeEvent, AlohaLinkInsertEvent, AlohaLinkRemoveEvent } from '@gentics/aloha-models';
import {
    GCNAlohaPlugin,
    GCNLinkCheckerAlohaPluigin,
    GCNLinkCheckerPluginSettings,
    TAB_ID_CONSTRUCTS,
    TAB_ID_LINK_CHECKER,
} from '@gentics/cms-integration-api-models';
import { ConstructCategory, ExternalLink, NodeFeature, TagType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Subscription, combineLatest, merge, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { AlohaGlobal } from '../../models/content-frame';
import {
    AlohaIntegrationService,
    NormalizedComponentGroup,
    NormalizedSlotDisplay,
    NormalizedTabsSettings,
    NormalizedToolbarSizeSettings,
} from '../../providers/aloha-integration/aloha-integration.service';
import { MobileMenu } from '../../utils';

const ATTR_ALOHA_REPO = 'data-gentics-aloha-repository'
const ATTR_QUEUED_LINK_CHECK = 'gcmsui-queued-link-check';
const DEFAULT_DELAY = 500;

function isJQueryElement(elem: any): elem is JQuery {
    return elem != null && typeof elem === 'object' && elem.length && typeof elem.attr === 'function';
}

function isInternalLink(element: HTMLElement | JQuery): boolean {
    if (element == null) {
        return false;
    }
    if (isJQueryElement(element)) {
        return element.attr(ATTR_ALOHA_REPO) != null;
    }
    return element.getAttribute(ATTR_ALOHA_REPO) != null;
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

@Component({
    selector: 'gtx-page-editor-controls',
    templateUrl: './page-editor-controls.component.html',
    styleUrls: ['./page-editor-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageEditorControlsComponent implements OnInit, AfterViewInit, OnDestroy {

    public readonly TAB_ID_CONSTRUCTS = TAB_ID_CONSTRUCTS;
    public readonly TAB_ID_LINK_CHECKER = TAB_ID_LINK_CHECKER;

    @Output()
    public brokenLinkCountChange = new EventEmitter<number>();

    @ViewChildren('mobileMenu')
    public menus: QueryList<HTMLElement[]>;

    public activeTab: string;
    public settings: NormalizedToolbarSizeSettings;
    public components: Record<string, AlohaComponent> = {};
    public gcnPlugin: GCNAlohaPlugin;
    public alohaRef: AlohaGlobal;
    public tagEditorOpen = false;
    public linkCheckerEnabled = false;
    public editable: AlohaEditable | null = null;

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
    protected currentMenus: MobileMenu[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected appState: ApplicationStateService,
        protected client: GCMSRestClientService,
        protected aloha: AlohaIntegrationService,
        protected userSettings: UserSettingsService,
    ) {}

    public ngOnInit(): void {
        this.activeTab = this.aloha.activeTab;

        this.subscriptions.push(combineLatest([
            this.client.construct.list(),
            this.aloha.activeEditable$.pipe(
                tap(editable => {
                    this.editable = editable;
                    this.changeDetector.markForCheck();
                }),
            ),
            this.aloha.settings$,
        ]).pipe(
            map(([res, activeEditable, settings]) => {
                const raw = res.items;

                // No element selected, nothing to do
                if (activeEditable?.obj?.[0] == null) {
                    return raw;
                }

                const elem = activeEditable.obj[0];
                const editables = settings?.plugins?.gcn?.editables || {};
                const whitelist = (Object.entries(editables).find(([selector]) => {
                    return elem.matches(selector);
                })?.[1] as any)?.tagtypeWhitelist;

                if (!Array.isArray(whitelist)) {
                    return raw;
                }

                return raw.filter(construct => whitelist.includes(construct.keyword));
            }),
        ).subscribe(constructs => {
            this.constructs = constructs;
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

        this.subscriptions.push(this.aloha.editorTab$.subscribe(activeTab => {
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

            this.subscriptions.push(this.aloha.require('gcnlinkchecker/gcnlinkchecker-plugin').subscribe(plugin => {
                if (plugin == null) {
                    this.linkCheckerPlugin = null;
                    return;
                }

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

        this.subscriptions.push(merge(
            this.aloha.on<AlohaLinkChangeEvent>('aloha.link.changed'),
            this.aloha.on<AlohaLinkChangeEvent>('gcn.link.changed'),
        ).subscribe(event => {
            if (!this.linkCheckerPlugin) {
                return;
            }

            if (event == null || event.element == null || event.element.length === 0) {
                return;
            }

            if (isInternalLink(event.element)) {
                this.removeCheckedDelay(event.element[0]);
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
                    this.removeCheckedDelay(linkElement);
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

    public ngAfterViewInit(): void {
        this.subscriptions.push(this.menus.changes.subscribe(changeObj => {
            this.currentMenus.forEach(s => s.destroy());
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call, no-underscore-dangle
            (changeObj._results || []).forEach(elem => {
                const menu = new MobileMenu(elem.nativeElement);
                menu.init();
                this.currentMenus.push(menu);
            });
        }));
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.currentMenus.forEach(s => s.destroy());
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

    protected removeCheckedDelay(element: HTMLElement | JQuery): void {
        if (isJQueryElement(element)) {
            element = element[0];
        }

        if (!element.hasAttribute(ATTR_QUEUED_LINK_CHECK)) {
            return;
        }
        const timerId = parseInt(element.getAttribute(ATTR_QUEUED_LINK_CHECK) || '', 10);
        element.removeAttribute(ATTR_QUEUED_LINK_CHECK);

        if (timerId && !Number.isNaN(timerId)) {
            window.clearTimeout(timerId);
        }
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
