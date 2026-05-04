import { Injectable, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { DevicePreset, DevicePreviewState } from '../../models/device-preset';

/**
 * Built-in default presets. Three formats covering mobile / tablet / desktop
 * (plus a desktop-XL bonus). Can be extended at runtime via custom presets.
 */
export const DEFAULT_DEVICE_PRESETS: ReadonlyArray<DevicePreset> = Object.freeze([
    { id: 'mobile',     labelKey: 'editor.device_mobile_label',     icon: 'smartphone',      width: 375,  height: 667 },
    { id: 'tablet',     labelKey: 'editor.device_tablet_label',     icon: 'tablet_mac',      width: 768,  height: 1024 },
    { id: 'desktop',    labelKey: 'editor.device_desktop_label',    icon: 'desktop_windows', width: 1200, height: 840 },
    { id: 'desktop-xl', labelKey: 'editor.device_desktop_xl_label', icon: 'tv',              width: 1600, height: 960 },
]);

/** Query-parameter name carrying the active preset id in the URL. */
export const DEVICE_PREVIEW_QUERY_PARAM = 'device';

/**
 * Holds the device-preview state (active flag + selected preset) and exposes
 * it as observables so the editor-toolbar and content-frame components stay
 * in sync.
 *
 * **State source of truth: the URL.** The active preset id lives as the
 * `?device=<id>` query parameter on the editor route. Reasons:
 *   - shareable links carry the user's preview format,
 *   - browser back/forward navigates between formats naturally,
 *   - no local persistence layer is needed (no UserSettings entry, no
 *     LocalStorage handling, no per-user/per-tab divergence).
 *
 * The service mirrors the URL into its `state$` BehaviorSubject so that
 * consumers don't have to deal with `ActivatedRoute` themselves. Mutations
 * are performed via Angular's `Router`, which means they integrate cleanly
 * with the rest of the editor's navigation flow.
 */
@Injectable({ providedIn: 'root' })
export class DevicePreviewService implements OnDestroy {

    private readonly stateSubject = new BehaviorSubject<DevicePreviewState>({
        active: false,
        presetId: null,
    });

    private readonly presetsSubject = new BehaviorSubject<DevicePreset[]>(
        [...DEFAULT_DEVICE_PRESETS],
    );

    private routerSubscription: Subscription;

    /** Stream of state changes (active flag, selected preset id). */
    public readonly state$: Observable<DevicePreviewState> = this.stateSubject.asObservable();

    /** Stream of available presets (defaults + future custom entries). */
    public readonly presets$: Observable<DevicePreset[]> = this.presetsSubject.asObservable();

    /**
     * Convenience stream that resolves the currently active preset object
     * (or `null` when device-preview is off).
     */
    public readonly activePreset$: Observable<DevicePreset | null> = this.stateSubject.pipe(
        map(s => s.active && s.presetId ? this.findPreset(s.presetId) ?? null : null),
        distinctUntilChanged((a, b) => a?.id === b?.id),
    );

    constructor(private router: Router) {
        // Seed initial state from the current URL.
        this.syncStateFromUrl(this.router.url);

        // Keep state in sync with future URL changes (programmatic navigation,
        // browser back/forward, links).
        this.routerSubscription = this.router.events.pipe(
            filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        ).subscribe((event) => {
            this.syncStateFromUrl(event.urlAfterRedirects);
        });
    }

    public ngOnDestroy(): void {
        this.routerSubscription?.unsubscribe();
    }

    /** Returns the latest synchronously-known state. */
    public get currentState(): DevicePreviewState {
        return this.stateSubject.value;
    }

    /** Returns the latest synchronously-known list of presets. */
    public get presets(): DevicePreset[] {
        return this.presetsSubject.value;
    }

    public findPreset(presetId: string): DevicePreset | undefined {
        return this.presetsSubject.value.find(p => p.id === presetId);
    }

    /**
     * Activate device-preview with the given preset by writing the preset id
     * into the URL's `?device=` query parameter. The state is then updated
     * via the router subscription.
     *
     * No-op if the preset id is unknown.
     */
    public activate(presetId: string): void {
        if (!this.findPreset(presetId)) {
            return;
        }
        this.updateDeviceQueryParam(presetId);
    }

    /**
     * Turn device-preview off by removing the `?device=` query parameter from
     * the URL. The state is then updated via the router subscription.
     */
    public deactivate(): void {
        if (!this.stateSubject.value.active) {
            return;
        }
        this.updateDeviceQueryParam(null);
    }

    /**
     * Toggle a preset on/off. Useful for menu items that act as both
     * activation and deactivation triggers for the same preset.
     */
    public toggle(presetId: string): void {
        const s = this.stateSubject.value;
        if (s.active && s.presetId === presetId) {
            this.deactivate();
        } else {
            this.activate(presetId);
        }
    }

    /* --------------------------------------------------------------------- *
     * Internal helpers
     * --------------------------------------------------------------------- */

    private syncStateFromUrl(url: string): void {
        const tree = this.router.parseUrl(url || '');
        const raw = tree.queryParams?.[DEVICE_PREVIEW_QUERY_PARAM];
        const presetId = typeof raw === 'string' ? raw : null;

        const next: DevicePreviewState = presetId && this.findPreset(presetId)
            ? { active: true, presetId }
            : { active: false, presetId: null };

        const current = this.stateSubject.value;
        if (current.active === next.active && current.presetId === next.presetId) {
            return;
        }
        this.stateSubject.next(next);
    }

    private updateDeviceQueryParam(presetId: string | null): void {
        const tree = this.router.parseUrl(this.router.url);
        if (presetId) {
            tree.queryParams = { ...tree.queryParams, [DEVICE_PREVIEW_QUERY_PARAM]: presetId };
        } else {
            const { [DEVICE_PREVIEW_QUERY_PARAM]: _omit, ...rest } = tree.queryParams || {};
            tree.queryParams = rest;
        }
        this.router.navigateByUrl(tree, { replaceUrl: true });
    }
}
