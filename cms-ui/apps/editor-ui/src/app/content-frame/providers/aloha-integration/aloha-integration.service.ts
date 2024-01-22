import { Injectable, NgZone } from '@angular/core';
import { DefaultEditorControlTabs, INVERSE_DEFAULT_EDITOR_TABS_MAPPING, PageEditorTab } from '@editor-ui/app/common/models';
import {
    AlohaButtonComponent,
    AlohaComponent,
    AlohaComponentSetting,
    AlohaCoreComponentNames,
    AlohaFullComponentSetting,
    AlohaRangeObject,
    AlohaSettings,
    AlohaToolbarSettings,
    AlohaToolbarSizeSettings,
    AlohaToolbarTabsSettings,
    GCNAlohaPlugin,
    ScreenSize,
} from '@gentics/aloha-models';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { AlohaGlobal } from '../../models/content-frame';

export interface NormalizedTabsSettings extends Omit<AlohaToolbarTabsSettings, 'components'> {
    components: AlohaFullComponentSetting[][];
}

export interface NormalizedToolbarSizeSettings extends AlohaToolbarSizeSettings {
    tabs: NormalizedTabsSettings[];
}

const MOCK_TOOLBAR_SIZE_SETTINGS: AlohaToolbarSizeSettings = {
    tabs: [
        {
            id: 'formatting',
            label: 'tab.format.label',
            icon: 'border_color',
            showOn: { scope: 'Aloha.continuoustext' },
            components: [
                ['bold', 'italic'], [ { slot: 'strike' }],
            ],
        },
    ],
};

const makeAlohaButton: (type: string, label: string, icon: string) => AlohaComponent = (type, label, icon) => {
    return {
        type,
        id: 0,
        disabled: false,
        isInstance: true,
        text: null,
        icon: icon,
        tooltip: label,
        touched: false,
        visible: true,
        validationErrors: null,

        adoptParent: () => {},
        click() {
            console.log('button has been clicked!', this);
        },
        closeTooltip: () => {},
        destroy: () => {},
        disable: () => {},
        enable: () => {},
        focus: () => {},
        foreground: () => {},
        getValue: () => {},
        hide: () => {},
        init: () => {},
        isValid: () => true,
        isVisible: () => true,
        setIcon(newIcon) {
            this.icon = newIcon;
        },
        setText(newText) {
            this.text = newText;
        },
        setTooltip(newTooltip) {
            this.tooltip = newTooltip;
        },
        setValue: () => {},
        show: () => {},
        touch: () => {},
        untouched: () => {},
        triggerChangeNotification: () => {},
        triggerTouchNotification: () => {},
        changeNotify: () => {},
        touchNotify: () => {},
    };
}

const MOCK_COMPONENTS: Record<string, AlohaComponent> = {
    bold: makeAlohaButton(AlohaCoreComponentNames.BUTTON, 'bold!', 'format_bold'),
    italic: makeAlohaButton(AlohaCoreComponentNames.TOGGLE_BUTTON, 'italiccccc!', 'format_italic'),
    strike: makeAlohaButton(AlohaCoreComponentNames.BUTTON, 'strikethrough', 'format_strikethrough'),
}

const MOCK_TOOLBAR_SETTINGS: AlohaToolbarSettings = {
    desktop: MOCK_TOOLBAR_SIZE_SETTINGS,
    mobile: MOCK_TOOLBAR_SIZE_SETTINGS,
    tablet: MOCK_TOOLBAR_SIZE_SETTINGS,
};

function normalizeToolbarSizeSettings(settings: AlohaToolbarSizeSettings): NormalizedToolbarSizeSettings {
    if (settings == null) {
        return null;
    }
    return {
        ...settings,
        tabs: settings.tabs.map(normalizeToolbarTab),
    };
}

function normalizeToolbarTab(tab: AlohaToolbarTabsSettings): NormalizedTabsSettings {
    let components = tab.components;
    if (!Array.isArray(components[0])) {
        components = components.map(comp => [comp]);
    }
    components = components.map(toMap => (toMap as AlohaComponentSetting[]).map(normalizeComponentDefinition));

    return {
        ...tab,
        // eslint-disable-next-line @typescript-eslint/restrict-template-expressions, @typescript-eslint/no-unsafe-call
        label: `aloha.${(tab.label || '').replaceAll('.', '_')}`,
        components: components as AlohaFullComponentSetting[][],
    };
}

function normalizeComponentDefinition(comp: AlohaComponentSetting): AlohaFullComponentSetting {
    if (comp == null) {
        return null;
    }
    if (typeof comp === 'string') {
        return { slot: comp };
    }
    return comp;
}

@Injectable()
export class AlohaIntegrationService {

    /*
     * Aloha objects-subjects which update whenever something in the IFrame changes.
     * Therefore syncing up to the UI.
     */
    public reference$ = new BehaviorSubject<AlohaGlobal>(null);
    public settings$ = new BehaviorSubject<AlohaSettings>(null);
    public contextChange$ = new BehaviorSubject<AlohaRangeObject>(null);
    public gcnPlugin$ = new BehaviorSubject<GCNAlohaPlugin>(null);
    public activeToolbarSettings$: Observable<NormalizedToolbarSizeSettings>;

    protected activeEditorSub = new BehaviorSubject<string>(null);
    protected editorChangeSub = new BehaviorSubject<void>(null);
    protected activeSizeSub = new BehaviorSubject<ScreenSize>(ScreenSize.DESKTOP);
    protected componentsSub = new BehaviorSubject<Record<string, AlohaComponent>>(MOCK_COMPONENTS);

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

    public activeEditor: string;
    public registeredComponents: Record<string, AlohaComponent> = MOCK_COMPONENTS;
    public editors: Record<string, PageEditorTab> = {};

    constructor(zone: NgZone) {
        zone.runOutsideAngular(() => {
            // TODO: Define the breakpoints somewhere static
            this.handleMedia('(max-width: 400px)', ScreenSize.MOBILE);
            this.handleMedia('(min-width: 401px) and (max-width: 1024px)', ScreenSize.TABLET);
            this.handleMedia('(min-width: 1025px)', ScreenSize.DESKTOP);
        });

        if (true) {
            this.activeToolbarSettings$ = of(MOCK_TOOLBAR_SIZE_SETTINGS).pipe(
                map(normalizeToolbarSizeSettings),
            );
        } else {
            this.activeToolbarSettings$ = combineLatest([
                this.settings$.asObservable(),
                this.size$,
            ]).pipe(
                filter(([settings, size]) => settings?.toolbar != null && size != null && settings.toolbar[size] != null),
                map(([settings, size]) => settings.toolbar[size]),
                map(normalizeToolbarSizeSettings),
            );
        }
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

        const tab = this.editors[id];
        // ID has to be either a gcmsui-tab or a custom tab to be able to activate.
        if (!DefaultEditorControlTabs[id] && !INVERSE_DEFAULT_EDITOR_TABS_MAPPING[id] && !tab) {
            return false;
        }

        // Cannot focus a disabled/hidden tab
        if (tab && (tab.disabled || tab.hidden)) {
            return false;
        }

        this.activeEditor = id;
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
