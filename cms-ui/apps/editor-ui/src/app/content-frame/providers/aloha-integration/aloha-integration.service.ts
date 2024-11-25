import { Injectable, NgZone } from '@angular/core';
import {
    AlohaComponent,
    AlohaComponentSetting,
    AlohaCoreComponentNames,
    AlohaEditable,
    AlohaFullComponentSetting,
    AlohaPubSub,
    AlohaRangeObject,
    AlohaScopeChangeEvent,
    AlohaScopes,
    AlohaSetEditableActiveEvent,
    AlohaSettings,
    AlohaToolbarSizeSettings,
    AlohaToolbarTabsSettings,
    AlohaUiPlugin,
    ScreenSize,
} from '@gentics/aloha-models';
import { GCNAlohaPlugin, TAB_ID_CONSTRUCTS, TAB_ID_LINK_CHECKER } from '@gentics/cms-integration-api-models';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, startWith, switchMap } from 'rxjs/operators';
import { BaseAlohaRendererComponent } from '../../components/base-aloha-renderer/base-aloha-renderer.component';
import { AlohaGlobal, CNWindow } from '../../models/content-frame';

export interface NormalizedSlotDisplay {
    name: string;
    visible: boolean;
}

export interface NormalizedComponentGroup {
    slots: NormalizedSlotDisplay[];
    hasVisibleSlots: boolean;
}

export interface NormalizedTabsSettings extends Omit<AlohaToolbarTabsSettings, 'components'> {
    components: AlohaFullComponentSetting[][];
    componentGroups: NormalizedComponentGroup[];
    hasVisibleGroups: boolean;
}

export interface NormalizedToolbarSizeSettings extends AlohaToolbarSizeSettings {
    tabs: NormalizedTabsSettings[];
}

export interface RequireOptions {
    skipReady?: boolean;
    allowNull?: boolean;
}

export const RENDERABLE_COMPONENTS: string[] = Object.values(AlohaCoreComponentNames);

const LINE_BREAK_COMPONENT = '\n';

/** Special scope which is to filter out that it should only be visible in the GCMS UI. */
const GCMSUI_SCOPE = 'gtx.gcmsui';

const TABS_TO_ALWAYS_DISPLAY = [TAB_ID_CONSTRUCTS, TAB_ID_LINK_CHECKER];

function inScope(scopesRef: AlohaScopes, scopeDef?: string | string[]): boolean {
    if (scopesRef == null) {
        return false;
    }

    if (scopeDef == null) {
        return true;
    } else if (typeof scopeDef === 'string') {
        return scopeDef === GCMSUI_SCOPE || scopesRef.isActiveScope(scopeDef);
    } else {
        return scopeDef.some(def => def === GCMSUI_SCOPE || scopesRef.isActiveScope(def));
    }
}

function normalizeComponentDefinition(comp: AlohaComponentSetting): AlohaFullComponentSetting {
    if (comp == null || comp === LINE_BREAK_COMPONENT || (comp as any)?.slot === LINE_BREAK_COMPONENT) {
        return null;
    }
    if (typeof comp === 'string') {
        return { slot: comp };
    }
    return comp;
}

function normalizeToolbarTab(
    tab: AlohaToolbarTabsSettings,
    globalComponents: Record<string, AlohaComponent>,
    scopesRef: AlohaScopes,
): NormalizedTabsSettings {
    let tabComponents = tab.components;
    if (tabComponents == null) {
        tabComponents = [];
    } else if (!Array.isArray(tabComponents[0])) {
        tabComponents = tabComponents.map(comp => [comp]);
    }

    tabComponents = tabComponents.map(toMap => (toMap as AlohaComponentSetting[])
        .map(normalizeComponentDefinition)
        .filter(comp => comp != null),
    ).filter(arr => (arr || []).length > 0);

    let hasVisibleGroups = TABS_TO_ALWAYS_DISPLAY.includes(tab.id);

    const groups: NormalizedComponentGroup[] = (tabComponents as AlohaFullComponentSetting[][])
        .map(arr => {
            let hasVisibleSlots = false;
            const slots = arr.map(comp => {
                const visible = inScope(scopesRef, comp.scope)
                    && RENDERABLE_COMPONENTS.includes(globalComponents[comp.slot]?.type)
                    && globalComponents[comp.slot]?.visible;
                hasVisibleSlots ||= visible;

                return {
                    name: comp.slot,
                    visible,
                };
            });

            hasVisibleGroups ||= hasVisibleSlots;

            return {
                hasVisibleSlots,
                slots,
            };
        });

    return {
        ...tab,
        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions, @typescript-eslint/no-unsafe-call
        label: tab.label,
        components: tabComponents as AlohaFullComponentSetting[][],
        componentGroups: groups,
        hasVisibleGroups,
    };
}

function normalizeToolbarSizeSettings(
    settings: AlohaToolbarSizeSettings,
    components: Record<string, AlohaComponent>,
    scopesRef: AlohaScopes,
): NormalizedToolbarSizeSettings {
    if (settings == null) {
        return null;
    }

    const tabs = (settings.tabs || [])
        .map(tabSettings => normalizeToolbarTab(tabSettings, components, scopesRef))
        .filter(tabSettings => inScope(scopesRef, tabSettings.showOn?.scope));

    return {
        ...settings,
        tabs,
    };
}

@Injectable({ providedIn: 'root' })
export class AlohaIntegrationService {

    /*
     * Aloha objects-subjects which update whenever something in the IFrame changes.
     * Therefore syncing up to the UI.
     */
    public reference$ = new BehaviorSubject<AlohaGlobal>(null);
    public settings$ = new BehaviorSubject<AlohaSettings>(null);
    public activeToolbarSettings$: Observable<NormalizedToolbarSizeSettings>;
    public ready$ = new BehaviorSubject<boolean>(false);
    /** Similar to {@link ready$}, but will be `true` even if the page fails. */
    public windowLoaded$ = new BehaviorSubject<boolean>(false);

    public gcnPlugin$: Observable<GCNAlohaPlugin>;
    public uiPlugin$: Observable<AlohaUiPlugin>;
    public scopesRef$: Observable<AlohaScopes>;
    public pubSubRef$: Observable<AlohaPubSub>;
    public contextChange$: Observable<AlohaRangeObject>;
    public scopeChange$: Observable<AlohaScopeChangeEvent>;

    protected editorTabSub = new BehaviorSubject<string>(null);
    protected activeSizeSub = new BehaviorSubject<ScreenSize>(ScreenSize.DESKTOP);
    protected componentsSub = new BehaviorSubject<Record<string, AlohaComponent>>({});
    protected activeEditableSub = new BehaviorSubject<AlohaEditable | null>(null);
    protected windowSub = new BehaviorSubject<CNWindow>(null);
    protected toolbarReloadSub = new BehaviorSubject<void>(null);

    /**
     * The currently selected/active editor in the page-controls.
     */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly editorTab$ = this.editorTabSub.asObservable();

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly size$ = this.activeSizeSub.asObservable();

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly activeEditable$ = this.activeEditableSub.asObservable();

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly components$ = this.componentsSub.asObservable();

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly window$ = this.windowSub.asObservable();

    public activeTab: string;
    public registeredComponents: Record<string, AlohaComponent> = {};
    public renderedComponents: Record<string, BaseAlohaRendererComponent<any, any>> = {};

    constructor(zone: NgZone) {
        zone.runOutsideAngular(() => {
            // TODO: Define the breakpoints somewhere static
            this.handleMedia('(max-width: 440px)', ScreenSize.MOBILE);
            this.handleMedia('(min-width: 441px) and (max-width: 1024px)', ScreenSize.TABLET);
            this.handleMedia('(min-width: 1025px)', ScreenSize.DESKTOP);
        });

        this.scopesRef$ = this.require('ui/scopes');
        this.pubSubRef$ = this.require('PubSub', { allowNull: true });
        this.uiPlugin$ = this.require('ui/ui-plugin');
        this.gcnPlugin$ = this.require('gcn/gcn-plugin');
        this.scopeChange$ = this.on('aloha.ui.scope.change');
        this.contextChange$ = this.on('aloha.selection.context-change');

        this.activeToolbarSettings$ = combineLatest([
            combineLatest([
                this.uiPlugin$,
                this.settings$.asObservable(), // Also needs a reload when global settings change
            ]).pipe(
                map(([plugin]) => plugin?.getToolbarSettings?.()),
                distinctUntilChanged(isEqual),
            ),
            this.size$,
            this.components$,
            this.scopesRef$,
            this.scopeChange$.pipe(
                // Needs an initial value, otherwise this entire `combineLatest` does not emit a value,
                // only when the user actualy performs a context-change.
                startWith(null),
            ),
            this.toolbarReloadSub.asObservable(),
        ]).pipe(
            map(([settings, size, components, scopesRef]) => {
                if (settings == null || size == null || settings[size] == null && scopesRef != null) {
                    return null;
                }
                return normalizeToolbarSizeSettings(settings[size], components, scopesRef);
            }),
            debounceTime(10),
        );

        this.on<AlohaSetEditableActiveEvent>('aloha.editable.set-active').pipe(
            map(event => {
                return event?.editable;
            }),
        ).subscribe(this.activeEditableSub);
    }

    public require<T = any>(resourceName: string, options?: RequireOptions): Observable<T> {
        return combineLatest([
            this.reference$.asObservable(),
            this.ready$.asObservable(),
        ]).pipe(
            filter(([ref, ready]) => (options?.allowNull || ref != null) && (options?.skipReady || ready)),
            map(([ref]) => {
                try {
                    return ref == null ? null : ref.require(resourceName);
                } catch (err) {
                    console.error('Could not require aloha resource:', resourceName);
                    return null;
                }
            }),
        );
    }

    /**
     * Aloha bind subscription
     * @param eventName Event name
     */
    public bind<T = any>(eventName: string): Observable<T> {
        return this.reference$.asObservable().pipe(
            distinctUntilChanged((a, b) => (a == null && b == null) || (a === b)),
            switchMap(ref => {
                if (ref == null) {
                    return of(null);
                }

                return new Observable<T>(sub => {
                    const handler = (event: T) => {
                        sub.next(event);
                    };
                    ref.bind(eventName, handler);

                    return () => ref.unbind(eventName, handler);
                });
            }),
        );
    }

    /**
     * PubSub "sub" subscription.
     * @param eventName Event name
     * @returns Observable which emits all the events
     */
    public on<T = any>(eventName: string): Observable<T> {
        return this.pubSubRef$.pipe(
            // eslint-disable-next-line @typescript-eslint/naming-convention
            switchMap((PubSub: AlohaPubSub) => {
                if (PubSub == null) {
                    return of(null);
                }

                return new Observable<T>(sub => {
                    const handler = (event: T) => {
                        sub.next(event);
                    };
                    PubSub.sub(eventName, handler);

                    return () => {
                        PubSub.unsub(eventName, handler);
                    }
                });
            }),
        );
    }

    private handleMedia(query: string, target: ScreenSize): void {
        const media = window.matchMedia(query);
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        const ref = this;

        media.onchange = function(change) {
            if (change.matches) {
                ref.activeSizeSub.next(target);
            }
        };

        if (media.matches) {
            this.activeSizeSub.next(target);
        }
    }

    public clearReferences(): void {
        this.ready$.next(false);
        this.windowLoaded$.next(false);
        this.reference$.next(null);
        this.settings$.next(null);
        this.registeredComponents = {};
        this.renderedComponents = {};
        this.componentsSub.next({});
    }

    public changeActivePageEditorTab(id: string): boolean {
        if (id == null) {
            return false;
        }

        this.activeTab = id;
        this.editorTabSub.next(id);

        return true;
    }

    public setWindow(window: CNWindow): void {
        this.windowSub.next(window);
    }

    public restoreSelection(): void {
        setTimeout(() => {
            this.windowSub.value?.focus?.();
        });
    }

    public registerComponent(slot: string, component: AlohaComponent): void {
        this.registeredComponents[slot] = component;
        this.componentsSub.next({ ...this.registeredComponents });
    }

    public unregisterComponent(slot: string): void {
        delete this.registeredComponents[slot];
        this.componentsSub.next({ ...this.registeredComponents });
    }

    public reloadToolbarSettings(): void {
        this.toolbarReloadSub.next();
    }
}
