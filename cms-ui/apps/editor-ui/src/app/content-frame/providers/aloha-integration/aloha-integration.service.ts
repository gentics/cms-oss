import { Injectable, NgZone } from '@angular/core';
import {
    AlohaComponent,
    AlohaComponentSetting,
    AlohaCoreComponentNames,
    AlohaFullComponentSetting,
    AlohaRangeObject,
    AlohaSettings,
    AlohaToolbarSizeSettings,
    AlohaToolbarTabsSettings,
    AlohaUiPlugin,
    GCNAlohaPlugin,
    ScreenSize,
} from '@gentics/aloha-models';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseAlohaRendererComponent } from '../../components/base-aloha-renderer/base-aloha-renderer.component';
import { AlohaGlobal } from '../../models/content-frame';

export interface NormalizedTabsSettings extends Omit<AlohaToolbarTabsSettings, 'components'> {
    components: AlohaFullComponentSetting[][];
    slotsToRender: string[][];
    hasSlotsToRender: boolean;
}

export interface NormalizedToolbarSizeSettings extends AlohaToolbarSizeSettings {
    tabs: NormalizedTabsSettings[];
}

export const RENDERABLE_COMPONENTS: string[] = Object.values(AlohaCoreComponentNames);

const LINE_BREAK_COMPONENT = '\n';
export const TAB_ID_CONSTRUCTS = 'gtx.constructs';
export const TAB_ID_LINK_CHECKER = 'gtx.link-checker';

const TABS_TO_IGNORE = [TAB_ID_CONSTRUCTS, TAB_ID_LINK_CHECKER];

function normalizeToolbarSizeSettings(
    settings: AlohaToolbarSizeSettings,
    components: Record<string, AlohaComponent>,
): NormalizedToolbarSizeSettings {
    if (settings == null) {
        return null;
    }
    return {
        ...settings,
        tabs: (settings.tabs || []).map(tabSettings => normalizeToolbarTab(tabSettings, components)),
    };
}

function normalizeToolbarTab(
    tab: AlohaToolbarTabsSettings,
    globalComponents: Record<string, AlohaComponent>,
): NormalizedTabsSettings {
    let tabComponents = tab.components;
    if (!Array.isArray(tabComponents[0])) {
        tabComponents = tabComponents.map(comp => [comp]);
    }
    tabComponents = tabComponents.map(toMap => (toMap as AlohaComponentSetting[])
        .map(normalizeComponentDefinition)
        .filter(comp => comp != null),
    ).filter(arr => (arr || []).length > 0);

    const slotsToRender = (tabComponents as AlohaFullComponentSetting[][])
        .map(arr => arr
            .map(comp => comp.slot)
            .filter(slot => TABS_TO_IGNORE.includes(tab.id) || RENDERABLE_COMPONENTS.includes(globalComponents[slot]?.type)),
        )
        .filter(arr => arr.length > 0);

    return {
        ...tab,
        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions, @typescript-eslint/no-unsafe-call
        label: `aloha.${(tab.label || '').replaceAll('.', '_')}`,
        components: tabComponents as AlohaFullComponentSetting[][],
        slotsToRender,
        hasSlotsToRender: slotsToRender.length > 0,
    };
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

@Injectable({ providedIn: 'root' })
export class AlohaIntegrationService {

    /*
     * Aloha objects-subjects which update whenever something in the IFrame changes.
     * Therefore syncing up to the UI.
     */
    public reference$ = new BehaviorSubject<AlohaGlobal>(null);
    public settings$ = new BehaviorSubject<AlohaSettings>(null);
    public contextChange$ = new BehaviorSubject<AlohaRangeObject>(null);
    public gcnPlugin$ = new BehaviorSubject<GCNAlohaPlugin>(null);
    public uiPlugin$ = new BehaviorSubject<AlohaUiPlugin>(null);
    public activeToolbarSettings$: Observable<NormalizedToolbarSizeSettings>;

    protected activeEditorSub = new BehaviorSubject<string>(null);
    protected editorChangeSub = new BehaviorSubject<void>(null);
    protected activeSizeSub = new BehaviorSubject<ScreenSize>(ScreenSize.DESKTOP);
    protected componentsSub = new BehaviorSubject<Record<string, AlohaComponent>>({});

    /**
     * The currently selected/active editor in the page-controls.
     */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly activeEditor$ = this.activeEditorSub.asObservable();
    /**
     * Observable which emits every time the `editors` has been changed.
     */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly editorsChange$ = this.editorChangeSub.asObservable();

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly size$ = this.activeSizeSub.asObservable();

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly components$ = this.componentsSub.asObservable();

    public activeTab: string;
    public registeredComponents: Record<string, AlohaComponent> = {};
    public renderedComponents: Record<string, BaseAlohaRendererComponent<any, any>> = {};

    constructor(zone: NgZone) {
        zone.runOutsideAngular(() => {
            // TODO: Define the breakpoints somewhere static
            this.handleMedia('(max-width: 400px)', ScreenSize.MOBILE);
            this.handleMedia('(min-width: 401px) and (max-width: 1024px)', ScreenSize.TABLET);
            this.handleMedia('(min-width: 1025px)', ScreenSize.DESKTOP);
        });

        this.activeToolbarSettings$ = combineLatest([
            combineLatest([
                this.uiPlugin$.asObservable().pipe(
                    map(plugin => plugin?.getToolbarSettings?.()),
                ),
                this.settings$.asObservable(), // Also needs a reload when global settings change
            ]).pipe(
                map(([toolbarSettings]) => toolbarSettings),
            ),
            this.size$,
            this.components$,
        ]).pipe(
            map(([settings, size, components]) => {
                if (settings == null || size == null || settings[size] == null) {
                    return null;
                }
                return normalizeToolbarSizeSettings(settings[size], components);
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
        this.reference$.next(null);
        this.settings$.next(null);
        this.contextChange$.next(null);
        this.gcnPlugin$.next(null);
        this.uiPlugin$.next(null);
        this.registeredComponents = {};
        this.renderedComponents = {};
    }

    public changeActivePageEditorTab(id: string): boolean {
        if (id == null) {
            return false;
        }

        this.activeTab = id;
        this.activeEditorSub.next(id);

        return true;
    }

    public registerComponent(slot: string, component: AlohaComponent): void {
        this.registeredComponents[slot] = component;
        this.componentsSub.next({ ...this.registeredComponents });
    }

    public unregisterComponent(slot: string): void {
        delete this.registeredComponents[slot];
    }
}
