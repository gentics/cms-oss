import { Injectable, NgZone } from '@angular/core';
import { DefaultEditorControlTabs, GCMSUI_EDITOR_TABS_NAMESPACE, INVERSE_DEFAULT_EDITOR_TABS_MAPPING, PageEditorTab } from '@editor-ui/app/common/models';
import { AlohaRangeObject, AlohaSettings, AlohaToolbarSizeSettings, GCNAlohaPlugin, ScreenSize } from '@gentics/aloha-models';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { AlohaGlobal } from '../../models/content-frame';

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
    public activeToolbarSettings$: Observable<AlohaToolbarSizeSettings>;

    protected activeEditorSub = new BehaviorSubject<string>(null);
    protected editorChangeSub = new BehaviorSubject<void>(null);
    protected activeSizeSub = new BehaviorSubject<ScreenSize>(ScreenSize.DESKTOP);

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

    public activeEditor: string;
    public editors: Record<string, PageEditorTab> = {};

    constructor(zone: NgZone) {
        zone.runOutsideAngular(() => {
            // TODO: Define the breakpoints somewhere static
            this.handleMedia('(max-width: 400px)', ScreenSize.MOBILE);
            this.handleMedia('(min-width: 401px) and (max-width: 1024px)', ScreenSize.TABLET);
            this.handleMedia('(min-width: 1025px)', ScreenSize.DESKTOP);
        });

        this.activeToolbarSettings$ = combineLatest([
            this.settings$.asObservable(),
            this.size$,
        ]).pipe(
            filter(([settings, size]) => settings?.toolbar != null && size != null && settings.toolbar[size] != null),
            map(([settings, size]) => settings.toolbar[size]),
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

    public registerPageEditorTab(tab: PageEditorTab): boolean {
        return this.doWithCustomTab(tab.id, (() => {
            // More type checkings?
            this.editors[tab.id] = tab;
            this.editorChangeSub.next();
        }), false);
    }

    public removePageEditorTab(id: string): boolean {
        return this.doWithCustomTab(id, () => {
            delete this.editors[id];
            this.editorChangeSub.next();

            if (this.activeEditor === id) {
                this.activeEditorSub.next(null);
            }
        });
    }

    public disablePageEditorTab(id: string): boolean {
        return this.doWithCustomTab(id, (tab) => {
            tab.disabled = true;
            if (this.activeEditor === id) {
                this.activeEditorSub.next(null);
            }
            this.editorChangeSub.next();
        });
    }

    public enablePageEditorTab(id: string): boolean {
        return this.doWithCustomTab(id, (tab) => {
            tab.disabled = false;
            this.editorChangeSub.next();
        });
    }

    public hidePageEditorTab(id: string): boolean {
        return this.doWithCustomTab(id, (tab) => {
            tab.hidden = true;
            if (this.activeEditor === id) {
                this.activeEditorSub.next(null);
            }
            this.editorChangeSub.next();
        });
    }

    public showPageEditorTab(id: string): boolean {
        return this.doWithCustomTab(id, (tab) => {
            tab.hidden = false;
            this.editorChangeSub.next();
        });
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

    protected doWithCustomTab(
        id: string,
        handler: (tab: PageEditorTab, id: string) => void | boolean,
        checkForExistance: boolean = true,
    ): boolean {
        const exists = typeof id === 'string' && !id.startsWith(GCMSUI_EDITOR_TABS_NAMESPACE) && this.editors[id];
        if (checkForExistance ? !exists : exists) {
            return false;
        }

        const value = handler(this.editors[id], id);
        if (value === false) {
            return false;
        }

        return true;
    }
}
