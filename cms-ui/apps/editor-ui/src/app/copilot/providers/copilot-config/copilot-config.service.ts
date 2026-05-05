import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import {
    CopilotAction,
    CopilotConfig,
    DEFAULT_COPILOT_CONFIG,
    I18nString,
} from '../../copilot.types';

/**
 * Loads the Content Copilot configuration from a JSON file dropped into
 * the customer's UI configuration folder.
 *
 * Mirrors the existing pattern used by `UIOverridesService` for
 * `ui-overrides.json`. Failure modes are deliberately silent (warn-only):
 * a missing or invalid file simply leaves the feature disabled, matching
 * how the rest of the editor-ui treats optional customer config.
 *
 * Provided at the root level (`providedIn: 'root'`) so the bootstrap-time
 * fetch survives `ContentFrameModule` being lazy-loaded; `load()` is
 * triggered from `AppComponent.ngOnInit()`.
 */
@Injectable({ providedIn: 'root' })
export class CopilotConfigService {

    private readonly configSubject = new BehaviorSubject<CopilotConfig>(DEFAULT_COPILOT_CONFIG);

    /** The current configuration. Emits at least once with the default. */
    public readonly config$: Observable<CopilotConfig> = this.configSubject.asObservable();

    /** Convenience selector for the master enable flag. */
    public readonly enabled$: Observable<boolean> = this.config$.pipe(
        map((cfg) => !!cfg.enabled),
    );

    /** Convenience selector for the configured action list. */
    public readonly actions$ = this.config$.pipe(map((cfg) => cfg.actions));

    constructor(private http: HttpClient) {}

    /** Snapshot of the current config (for non-reactive consumers). */
    public get config(): CopilotConfig {
        return this.configSubject.value;
    }

    /**
     * Fetch and parse `{ui-conf}/copilot.json`.
     *
     * Triggered once during application bootstrap. The cache-buster (`?t=`)
     * follows the convention from `UIOverridesService` so a customer dropping
     * a new file in does not have to wait for cache invalidation.
     */
    public load(): void {
        const url = `${CUSTOMER_CONFIG_PATH}copilot.json?t=${Date.now()}`;

        this.http.get(url, { responseType: 'text' }).pipe(
            map((body) => parseCopilotConfig(body)),
            catchError((err) => {
                // 404 is the expected "no copilot configured" case — stay quiet.
                if (err && err.status !== 404) {
                    // eslint-disable-next-line no-console
                    console.warn('[Copilot] could not load copilot.json:', err);
                }
                return of(DEFAULT_COPILOT_CONFIG);
            }),
        ).subscribe((cfg) => this.configSubject.next(cfg));
    }
}

/**
 * Parses the raw response body and validates the minimal contract.
 *
 * Anything unexpected → warn + fall back to the disabled default. The
 * service intentionally does not throw: customers should never lose their
 * editor because of a typo in `copilot.json`.
 */
function parseCopilotConfig(body: string): CopilotConfig {
    if (!body || !body.trim()) {
        return { ...DEFAULT_COPILOT_CONFIG };
    }

    let parsed: unknown;
    try {
        parsed = JSON.parse(body);
    } catch (err) {
        // eslint-disable-next-line no-console
        console.warn('[Copilot] copilot.json is not valid JSON, ignoring:', err);
        return { ...DEFAULT_COPILOT_CONFIG };
    }

    if (!parsed || typeof parsed !== 'object') {
        return { ...DEFAULT_COPILOT_CONFIG };
    }

    const raw = parsed as Partial<CopilotConfig>;
    const enabled = !!raw.enabled;
    const actions = Array.isArray(raw.actions) ? validateActions(raw.actions) : [];

    if (actions === null) {
        return { ...DEFAULT_COPILOT_CONFIG };
    }

    return { enabled, actions };
}

/**
 * Returns the validated action list, or `null` if any action breaks the
 * contract (in which case the caller falls back to the default config).
 */
function validateActions(input: unknown[]): CopilotAction[] | null {
    const out: CopilotAction[] = [];

    for (const candidate of input) {
        if (!candidate || typeof candidate !== 'object') {
            // eslint-disable-next-line no-console
            console.warn('[Copilot] action entry is not an object, ignoring config');
            return null;
        }
        const a = candidate as Partial<CopilotAction>;
        if (typeof a.id !== 'string' || !a.id) {
            // eslint-disable-next-line no-console
            console.warn('[Copilot] action is missing required string `id`, ignoring config');
            return null;
        }
        if (!isI18nString(a.labelI18n)) {
            // eslint-disable-next-line no-console
            console.warn(`[Copilot] action '${a.id}' is missing a valid 'labelI18n' map, ignoring config`);
            return null;
        }
        if (a.descriptionI18n != null && !isI18nString(a.descriptionI18n)) {
            // eslint-disable-next-line no-console
            console.warn(`[Copilot] action '${a.id}' has an invalid 'descriptionI18n' map, ignoring config`);
            return null;
        }
        const action: CopilotAction = {
            id: a.id,
            labelI18n: a.labelI18n,
        };
        if (a.icon != null) action.icon = String(a.icon);
        if (a.descriptionI18n != null) action.descriptionI18n = a.descriptionI18n;
        if (a.prompt != null) action.prompt = String(a.prompt);
        out.push(action);
    }

    return out;
}

function isI18nString(value: unknown): value is I18nString {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        return false;
    }
    const entries = Object.entries(value as Record<string, unknown>);
    if (entries.length === 0) {
        return false;
    }
    return entries.every(([key, v]) => typeof key === 'string' && typeof v === 'string');
}
