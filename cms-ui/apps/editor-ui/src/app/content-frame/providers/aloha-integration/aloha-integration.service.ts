import { Injectable, NgZone } from '@angular/core';
import {
    AlohaComponent,
    AlohaComponentSetting,
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
import { filter, map } from 'rxjs/operators';
import { AlohaGlobal } from '../../models/content-frame';
import { BaseAlohaRendererComponent } from '../../components/base-aloha-renderer/base-aloha-renderer.component';

export interface NormalizedTabsSettings extends Omit<AlohaToolbarTabsSettings, 'components'> {
    components: AlohaFullComponentSetting[][];
}

export interface NormalizedToolbarSizeSettings extends AlohaToolbarSizeSettings {
    tabs: NormalizedTabsSettings[];
}

const LINE_BREAK_COMPONENT = '\n';

function normalizeToolbarSizeSettings(settings: AlohaToolbarSizeSettings): NormalizedToolbarSizeSettings {
    if (settings == null) {
        return null;
    }
    return {
        ...settings,
        tabs: (settings.tabs || []).map(normalizeToolbarTab),
    };
}

function normalizeToolbarTab(tab: AlohaToolbarTabsSettings): NormalizedTabsSettings {
    let components = tab.components;
    if (!Array.isArray(components[0])) {
        components = components.map(comp => [comp]);
    }
    components = components.map(toMap => (toMap as AlohaComponentSetting[])
        .map(normalizeComponentDefinition)
        .filter(comp => comp != null),
    ).filter(arr => (arr || []).length > 0);

    return {
        ...tab,
        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions, @typescript-eslint/no-unsafe-call
        label: `aloha.${(tab.label || '').replaceAll('.', '_')}`,
        components: components as AlohaFullComponentSetting[][],
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
            this.uiPlugin$.asObservable().pipe(
                map(plugin => plugin?.getToolbarSettings?.()),
            ),
            this.size$,
            this.settings$.asObservable(), // Also needs a reload when global settings change
        ]).pipe(
            filter(([settings, size]) => settings != null && size != null && settings[size] != null),
            map(([settings, size]) => settings[size]),
            map(normalizeToolbarSizeSettings),
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
