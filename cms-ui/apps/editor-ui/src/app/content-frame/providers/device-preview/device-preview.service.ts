import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
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

/** LocalStorage key for the most recently used preset id. */
export const DEVICE_PREVIEW_LAST_PRESET_KEY = 'devicePreview.lastPresetId';

/**
 * Holds the device-preview state (active flag + selected preset) and exposes
 * it as observables so the editor-toolbar and content-frame components stay in
 * sync.
 *
 * Persistence: the most recently used preset is stored via the existing
 * `LocalStorage` wrapper so the user's last choice survives a reload.
 *
 * Note on architecture: kept intentionally lightweight (BehaviorSubject) so
 * it does not pull in the global NgRx state graph for a UI-only concern.
 * If CMS-wide configurable presets become a requirement later, the `presets$`
 * observable is the natural extension point.
 */
@Injectable({ providedIn: 'root' })
export class DevicePreviewService {

    private readonly stateSubject = new BehaviorSubject<DevicePreviewState>({
        active: false,
        presetId: null,
    });

    private readonly presetsSubject = new BehaviorSubject<DevicePreset[]>(
        [...DEFAULT_DEVICE_PRESETS],
    );

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

    constructor(private localStorage: LocalStorage) {}

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

    /** Activate device-preview with the given preset and persist the choice. */
    public activate(presetId: string): void {
        const preset = this.findPreset(presetId);
        if (!preset) {
            return;
        }
        this.stateSubject.next({ active: true, presetId });
        this.persistLastPresetId(presetId);
    }

    /** Turn device-preview off (returns to full-width content-frame). */
    public deactivate(): void {
        if (!this.stateSubject.value.active) {
            return;
        }
        this.stateSubject.next({ active: false, presetId: null });
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

    /** Returns the most recently used preset id, or null. */
    public getLastUsedPresetId(): string | null {
        const value = this.localStorage.getForAllUsers(DEVICE_PREVIEW_LAST_PRESET_KEY);
        return typeof value === 'string' ? value : null;
    }

    /**
     * Activate the last-used preset if any was persisted; otherwise no-op.
     * Useful when the user clicks the preview-button without picking a format.
     */
    public activateLastUsed(): boolean {
        const last = this.getLastUsedPresetId();
        if (last && this.findPreset(last)) {
            this.activate(last);
            return true;
        }
        return false;
    }

    private persistLastPresetId(id: string): void {
        try {
            this.localStorage.setForAllUsers(DEVICE_PREVIEW_LAST_PRESET_KEY, id);
        } catch {
            // ignore — persistence is a nice-to-have, not critical
        }
    }
}
