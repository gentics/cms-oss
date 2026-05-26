import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { I18nNotificationService, I18nService } from '@gentics/cms-components';
import { FormTranslations, FormTranslationsLanguage } from '@gentics/cms-models';
import { firstValueFrom } from 'rxjs';

import { AuthenticationService } from '../../core/services/authentication.service';
import { ToolApiService } from '../../core/services/tool-api.service';
import {
    FilterMode,
    GLOBAL_SCOPE_ID,
    Scope,
    ScopeId,
} from '../../models/translations.model';
import { FormTranslationsApiService } from '../../services/form-translations-api.service';
import { ScopeTabInfo } from '../scope-tabs/scope-tabs.component';
import { CellEditEvent } from '../translations-table/translations-table.component';

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

/**
 * The shell owns ALL state for this small tool — there is no separate store.
 * Per review feedback, the BehaviorSubject-based service was over-engineered
 * for the size of this UI.
 */
@Component({
    selector: 'gtx-shell',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './shell.component.html',
    styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit {

    private readonly auth = inject(AuthenticationService);
    private readonly toolApi = inject(ToolApiService);
    private readonly api = inject(FormTranslationsApiService);
    private readonly notifications = inject(I18nNotificationService);
    private readonly i18n = inject(I18nService);

    // -------- bootstrap state --------
    bootstrapStatus: LoadStatus = 'idle';
    hasSession = false;
    sid: string | null = null;

    // -------- view state --------
    languages: FormTranslationsLanguage[] = [];
    scopes: Scope[] = [];
    activeScopeId: ScopeId = GLOBAL_SCOPE_ID;
    search = '';
    filter: FilterMode = 'all';
    saving = false;

    // -------- translations state per scope --------
    private saved: Record<string, FormTranslations> = {};
    private draft: Record<string, FormTranslations> = {};
    private scopeStatus: Record<string, LoadStatus> = {};

    ngOnInit(): void {
        void this.bootstrap();
    }

    /* =====================================================================
     *  Bootstrap
     * ===================================================================== */

    private async bootstrap(): Promise<void> {
        this.bootstrapStatus = 'loading';

        this.sid = this.auth.sid;
        this.hasSession = this.sid != null;
        if (!this.hasSession) {
            this.bootstrapStatus = 'loaded';
            return;
        }

        /* Pull-based protocol: expose hasUnsavedChanges() so the embedding
           UI can ask whenever it wants (e.g. on navigation). */
        this.toolApi.initialize({
            hasUnsavedChanges: () => this.dirtyCount > 0,
        });

        try {
            const [languages, formTypes, globalTranslations] = await Promise.all([
                firstValueFrom(this.api.loadLanguages()),
                firstValueFrom(this.api.loadFormTypes()),
                firstValueFrom(this.api.loadGlobalTranslations()),
            ]);

            this.languages = languages;
            this.scopes = [
                {
                    id: GLOBAL_SCOPE_ID,
                    label: this.i18n.instant('SCOPE.GLOBAL_TITLE'),
                    description: 'SCOPE.GLOBAL_DESC',
                    isGlobal: true,
                },
                ...formTypes.map(type => ({
                    id: type.type,
                    label: this.pickI18n(type.nameI18n) ?? type.type,
                    description: 'SCOPE.TYPE_DESC',
                    isGlobal: false,
                })),
            ];
            this.saved[GLOBAL_SCOPE_ID] = globalTranslations;
            this.draft[GLOBAL_SCOPE_ID] = clone(globalTranslations);
            this.scopeStatus[GLOBAL_SCOPE_ID] = 'loaded';

            this.bootstrapStatus = 'loaded';
        } catch (err) {
            this.bootstrapStatus = 'error';
            this.notifications.show({ type: 'alert', message: 'NOTIFY.LOAD_ERROR' });
            // eslint-disable-next-line no-console
            console.error('form-translations bootstrap failed', err);
        }
    }

    private async loadScopeIfNeeded(scopeId: ScopeId): Promise<void> {
        if (this.scopeStatus[scopeId] === 'loaded' || scopeId === GLOBAL_SCOPE_ID) {
            return;
        }
        this.scopeStatus[scopeId] = 'loading';
        try {
            const data = await firstValueFrom(this.api.loadTypeTranslations(scopeId));
            this.saved[scopeId] = data;
            this.draft[scopeId] = clone(data);
            this.scopeStatus[scopeId] = 'loaded';
        } catch (err) {
            this.scopeStatus[scopeId] = 'error';
            this.notifications.show({
                type: 'alert',
                message: 'NOTIFY.SCOPE_LOAD_ERROR',
                translationParams: { scope: this.scopes.find(s => s.id === scopeId)?.label ?? scopeId },
            });
            // eslint-disable-next-line no-console
            console.error('scope load failed', err);
        }
    }

    /* =====================================================================
     *  Derived state (read by the template)
     * ===================================================================== */

    get activeScope(): Scope | null {
        return this.scopes.find(s => s.id === this.activeScopeId) ?? null;
    }

    get activeScopeLoading(): boolean {
        return this.scopeStatus[this.activeScopeId] === 'loading';
    }

    get placeholderKeys(): string[] {
        const set = new Set<string>();
        for (const map of Object.values(this.saved)) {
            for (const key of Object.keys(map)) set.add(key);
        }
        for (const map of Object.values(this.draft)) {
            for (const key of Object.keys(map)) set.add(key);
        }
        return [...set].sort((a, b) => a.localeCompare(b));
    }

    get visibleKeys(): string[] {
        const all = this.placeholderKeys;
        const q = this.search.trim().toLowerCase();
        const draft = this.draft[this.activeScopeId] ?? {};
        return all.filter(key => {
            if (q && !key.toLowerCase().includes(q)) return false;
            if (this.filter === 'incomplete') {
                const incomplete = this.languages.some(l => (draft[key]?.[l.code] ?? '').trim() === '');
                if (!incomplete) return false;
            }
            return true;
        });
    }

    get totalKeys(): number {
        return this.placeholderKeys.length;
    }

    get dirtyCount(): number {
        let n = 0;
        for (const scopeId of Object.keys(this.draft)) {
            n += countCells(diff(this.saved[scopeId] ?? {}, this.draft[scopeId] ?? {}));
        }
        return n;
    }

    get scopeTabs(): ScopeTabInfo[] {
        const totalKeys = this.placeholderKeys.length;
        const langs = this.languages.length;
        return this.scopes.map(scope => {
            const draft = this.draft[scope.id] ?? {};
            let translatedCount = 0;
            for (const row of Object.values(draft)) {
                for (const val of Object.values(row)) {
                    if (val.trim() !== '') translatedCount++;
                }
            }
            return {
                scope,
                translatedCount,
                totalCount: totalKeys * langs,
                hasDirty: countCells(diff(this.saved[scope.id] ?? {}, draft)) > 0,
            };
        });
    }

    get activeDraft(): FormTranslations {
        return this.draft[this.activeScopeId] ?? {};
    }

    get activeSaved(): FormTranslations {
        return this.saved[this.activeScopeId] ?? {};
    }

    /* =====================================================================
     *  User actions
     * ===================================================================== */

    async onScopeSelect(scopeId: ScopeId): Promise<void> {
        if (scopeId === this.activeScopeId) return;
        if (this.hasDirtyInScope(this.activeScopeId)) {
            const confirmed = window.confirm(this.i18n.instant('SAVE_BAR.CONFIRM_SCOPE_SWITCH'));
            if (!confirmed) return;
            this.draft[this.activeScopeId] = clone(this.saved[this.activeScopeId] ?? {});
        }
        this.activeScopeId = scopeId;
        await this.loadScopeIfNeeded(scopeId);
        /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
    }

    onSearchChange(term: string): void {
        this.search = term;
    }

    onFilterChange(filter: FilterMode): void {
        this.filter = filter;
    }

    onCellEdit(event: CellEditEvent): void {
        const scopeId = this.activeScopeId;
        const draftForScope = this.draft[scopeId] ?? {};
        const row = draftForScope[event.key] ?? {};
        this.draft = {
            ...this.draft,
            [scopeId]: {
                ...draftForScope,
                [event.key]: { ...row, [event.langCode]: event.value },
            },
        };
        /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
    }

    async onSave(): Promise<void> {
        const scopeId = this.activeScopeId;
        const scope = this.activeScope;
        if (!scope) return;
        const delta = diff(this.saved[scopeId] ?? {}, this.draft[scopeId] ?? {});
        const cellCount = countCells(delta);
        if (cellCount === 0) return;

        this.saving = true;
        try {
            const obs = scope.isGlobal
                ? this.api.saveGlobalTranslations(delta)
                : this.api.saveTypeTranslations(scopeId, delta);
            await firstValueFrom(obs);

            const merged = merge(this.saved[scopeId] ?? {}, delta);
            this.saved[scopeId] = merged;
            this.draft[scopeId] = clone(merged);

            this.notifications.show({
                type: 'success',
                message: 'NOTIFY.SAVE_SUCCESS',
                translationParams: { count: cellCount },
            });
            /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
        } catch (err) {
            this.notifications.show({ type: 'alert', message: 'NOTIFY.SAVE_ERROR' });
            // eslint-disable-next-line no-console
            console.error('save failed', err);
        } finally {
            this.saving = false;
        }
    }

    onDiscard(): void {
        if (!window.confirm(this.i18n.instant('SAVE_BAR.CONFIRM_DISCARD'))) return;
        const scopeId = this.activeScopeId;
        this.draft[scopeId] = clone(this.saved[scopeId] ?? {});
        this.notifications.show({ type: 'default', message: 'NOTIFY.DISCARD_DONE' });
        /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
    }

    /* =====================================================================
     *  Helpers
     * ===================================================================== */

    private hasDirtyInScope(scopeId: ScopeId): boolean {
        return countCells(diff(this.saved[scopeId] ?? {}, this.draft[scopeId] ?? {})) > 0;
    }

    private pickI18n(value: Record<string, string> | undefined): string | null {
        if (!value) return null;
        const lang = this.i18n.getCurrentLanguage();
        return value[lang] ?? value['en'] ?? Object.values(value)[0] ?? null;
    }
}

/* =====================================================================
 *  Pure utilities (kept at module level — no side effects)
 * ===================================================================== */

function clone<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
}

function diff(saved: FormTranslations, draft: FormTranslations): FormTranslations {
    const result: FormTranslations = {};
    const keys = new Set([...Object.keys(saved), ...Object.keys(draft)]);
    for (const key of keys) {
        const a = saved[key] ?? {};
        const b = draft[key] ?? {};
        const langs = new Set([...Object.keys(a), ...Object.keys(b)]);
        const row: Record<string, string> = {};
        for (const lang of langs) {
            const av = a[lang] ?? '';
            const bv = b[lang] ?? '';
            if (av !== bv) row[lang] = bv;
        }
        if (Object.keys(row).length > 0) result[key] = row;
    }
    return result;
}

function merge(base: FormTranslations, delta: FormTranslations): FormTranslations {
    const result: FormTranslations = { ...base };
    for (const [key, langs] of Object.entries(delta)) {
        result[key] = { ...(result[key] ?? {}), ...langs };
    }
    return result;
}

function countCells(map: FormTranslations): number {
    let n = 0;
    for (const row of Object.values(map)) n += Object.keys(row).length;
    return n;
}
