import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { CopilotConfig, DEFAULT_COPILOT_CONFIG } from '../../copilot.types';
import { parseCopilotYaml } from './copilot-yaml.parser';

/**
 * Loads the Content Copilot configuration from a YAML file dropped
 * into the customer's UI configuration folder.
 *
 * Mirrors the existing pattern used by `UIOverridesService` for
 * `ui-overrides.json`, but reads YAML so non-developers can edit it
 * comfortably.
 *
 * Failure modes are deliberately silent (warn-only): if the file is
 * missing or invalid, the feature simply stays disabled. This matches
 * how the rest of the editor-ui treats optional customer config.
 *
 * Provided at the root level (`providedIn: 'root'`) on purpose: the
 * `ContentFrameModule` that consumes the toolbar button is itself
 * lazy-loaded, so a module-scoped registration would silently miss
 * the bootstrap-only `provideAppInitializer` window. Triggering
 * `load()` from `AppComponent.ngOnInit()` keeps the kick-off in step
 * with the existing `UIOverridesService` pattern.
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
     * Fetch and parse `{ui-conf}/config/copilot.yml`.
     *
     * Triggered once during application bootstrap. The cache-buster
     * (`?t=`) follows the convention from `UIOverridesService` so a
     * customer dropping a new file in does not have to wait for cache
     * invalidation.
     */
    public load(): void {
        const url = `${CUSTOMER_CONFIG_PATH}config/copilot.yml?t=${Date.now()}`;

        this.http.get(url, { responseType: 'text' }).pipe(
            map((yaml) => parseCopilotYaml(yaml)),
            catchError((err) => {
                // 404 is the expected "no copilot configured" case — stay quiet.
                if (err && err.status !== 404) {
                    // eslint-disable-next-line no-console
                    console.warn('[Copilot] could not load copilot.yml:', err);
                }
                return of(DEFAULT_COPILOT_CONFIG);
            }),
        ).subscribe((cfg) => this.configSubject.next(cfg));
    }
}
