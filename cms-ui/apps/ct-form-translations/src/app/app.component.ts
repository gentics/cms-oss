import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    computed,
    inject,
    signal,
} from '@angular/core';
import { I18nNotificationService, I18nService } from '@gentics/cms-components';
import { FormTranslations, FormTranslationsLanguage, FormTypeConfiguration } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { firstValueFrom } from 'rxjs';
import {
    FilterMode,
    GLOBAL_SCOPE_ID,
    Scope,
    ScopeId,
} from './models/translations.model';
import { FormTranslationsApiService } from './services/form-translations-api.service';
import { ToolApiService } from './services/tool-api.service';
import { ScopeTabInfo } from './components/scope-tabs/scope-tabs.component';
import { CellEditEvent } from './components/translations-table/translations-table.component';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'no-session' | 'error';

@Component({
    selector: 'gtx-app',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class AppComponent implements OnInit {

    private readonly toolApi = inject(ToolApiService);
    private readonly api = inject(FormTranslationsApiService);
    private readonly modals = inject(ModalService);
    private readonly notifications = inject(I18nNotificationService);
    private readonly i18n = inject(I18nService);
    private readonly changeDetector = inject(ChangeDetectorRef);

    // -------- view state --------
    public readonly bootstrapStatus = signal<LoadStatus>('idle');
    public readonly languages = signal<FormTranslationsLanguage[]>([]);
    public readonly formTypeConfigurations = signal<FormTypeConfiguration[]>([]);
    public readonly savedTranslations = signal<Record<string, FormTranslations>>({});
    public readonly draft = signal<FormTranslations>({});

    public readonly activeScopeId = signal<string>(GLOBAL_SCOPE_ID);
    public readonly search = signal('');
    public readonly filter = signal<FilterMode>('all');
    public readonly saving = signal(false);

    // -------- computed state --------
    public readonly scopes = computed<Record<string, Scope>>(() => {
        const map: Record<string, Scope> = {
            [GLOBAL_SCOPE_ID]: {
                id: GLOBAL_SCOPE_ID,
                label: this.i18n.instant('tool.global_translations_title'),
                description: 'tool.global_translations_desc',
                isGlobal: true,
            },
        };

        for (const conf of this.formTypeConfigurations()) {
            map[conf.type] = {
                id: conf.type,
                label: this.i18n.fromObject(conf.nameI18n),
                description: 'tool.scope_translations_desc',
                isGlobal: false,
            };
        }

        return map;
    });

    public readonly activeScope = computed(() => {
        return this.scopes()[this.activeScopeId()];
    });

    public readonly activeTranslations = computed(() => {
        return this.savedTranslations()[this.activeScopeId()];
    });

    public readonly activeKeys = computed<string[]>(() => {
        return Object.keys(this.activeTranslations());
    });

    public readonly visibleKeys = computed<string[]>(() => {
        const all = this.activeKeys();
        const search = this.search();
        try {
            const q = (search || '').trim().toLowerCase();
            const data = merge(this.activeTranslations(), this.draft());
            const filter = this.filter();

            return all.filter((key) => {
                if (q && !key.toLowerCase().includes(q)) {
                    return false;
                }
                if (filter === 'incomplete') {
                    const incomplete = this.languages().some((l) => (data[key]?.[l.code] ?? '').trim() === '');
                    if (!incomplete) {
                        return false;
                    }
                }
                return true;
            });
        } catch (err) {
            debugger;
            return [];
        }
    });

    public readonly dirtyCount = computed(() => {
        return Object.keys(this.draft()).length;
    });

    public readonly scopeTabs = computed<ScopeTabInfo[]>(() => {
        const langs = this.languages().length;
        const active = this.activeScopeId();
        const translations = this.savedTranslations();
        const dirtyCount = this.dirtyCount();

        return Object.values(this.scopes()).map((scope) => {
            const isActive = scope.id === active;
            const translationData: FormTranslations = (isActive)
                ? merge(translations[scope.id], this.draft())
                : translations[scope.id];
            const totalKeys = Object.keys(translationData).length;

            let translatedCount = 0;
            for (const row of Object.values(translationData)) {
                for (const val of Object.values(row)) {
                    if (val.trim() !== '') translatedCount++;
                }
            }

            return {
                scope,
                translatedCount,
                totalCount: totalKeys * langs,
                hasDirty: isActive ? dirtyCount > 0 : false,
            };
        });
    });

    /* =====================================================================
    *  Bootstrap
    * ===================================================================== */

    ngOnInit(): void {
        this.bootstrap();
    }

    private async bootstrap(): Promise<void> {
        this.bootstrapStatus.set('loading');

        /* Pull-based protocol: expose hasUnsavedChanges() so the embedding
           UI can ask whenever it wants (e.g. on navigation). */
        this.toolApi.initialize({
            hasUnsavedChanges: () => Object.keys(this.draft()).length > 0,
        });

        try {
            const [languages, formTypeConfigurations, globalTranslations] = await Promise.all([
                firstValueFrom(this.api.loadLanguages()),
                firstValueFrom(this.api.loadFormTypes()),
                firstValueFrom(this.api.loadGlobalTranslations()),
            ]);

            this.languages.set(languages);
            this.formTypeConfigurations.set(formTypeConfigurations);

            const translationMap: Record<string, FormTranslations> = {
                [GLOBAL_SCOPE_ID]: globalTranslations,
            };

            const typeTranslations = await Promise.all(formTypeConfigurations.map((formTypeConfig) => {
                return firstValueFrom(this.api.loadTypeTranslations(formTypeConfig.type)).then((trans) => ({
                    type: formTypeConfig.type,
                    translations: trans,
                }));
            }));

            for (const entry of typeTranslations) {
                translationMap[entry.type] = entry.translations;
            }

            this.savedTranslations.set(translationMap);
            this.draft.set({});
            this.bootstrapStatus.set('loaded');
        } catch (err) {
            if (err instanceof GCMSRestClientRequestError) {
                // If we aren't logged in correctly
                if (err.responseCode === 401) {
                    this.bootstrapStatus.set('no-session');
                    return;
                }
            }
            this.bootstrapStatus.set('error');
            this.notifications.show({ type: 'alert', message: 'tool.translation_load_error' });

            console.error('form-translations bootstrap failed', err);
        } finally {
            this.changeDetector.markForCheck();
        }
    }

    /* =====================================================================
     *  User actions
     * ===================================================================== */

    async onScopeSelect(scopeId: ScopeId): Promise<void> {
        if (scopeId === this.activeScopeId()) {
            return;
        }

        if (this.dirtyCount() > 0) {
            const confirmed = await this.askDiscard();
            if (!confirmed) {
                return;
            }
            this.draft.set({});
        }
        this.activeScopeId.set(scopeId);
        /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
    }

    async askDiscard(): Promise<boolean> {
        const dialog = await this.modals.dialog({
            title: this.i18n.instant('tool.confirm_discard_title'),
            body: this.i18n.instant('tool.confirm_scope_switch_desc'),
            buttons: [
                {
                    id: 'cancel',
                    label: this.i18n.instant('common.cancel_button'),
                    type: 'secondary',
                    returnValue: false,
                },
                {
                    id: 'confirm',
                    label: this.i18n.instant('common.discard_button'),
                    type: 'alert',
                    returnValue: true,
                },
            ],
        });
        return await dialog.open();
    }

    onSearchChange(term: string): void {
        this.search.set(term || '');
    }

    onFilterChange(filter: FilterMode): void {
        this.filter.set(filter);
    }

    onCellEdit(event: CellEditEvent): void {
        this.draft.update((data) => {
            data[event.key] = {
                ...data[event.key],
                [event.langCode]: event.value,
            };
            // Needs to be new object for change detection to kick in
            return structuredClone(data);
        });
        /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
    }

    async onSave(): Promise<void> {
        const scopeId = this.activeScopeId();
        const scope = this.activeScope();
        if (!scope) {
            return;
        }

        const changeCount = this.dirtyCount();
        if (changeCount === 0) {
            return;
        }

        this.saving.set(true);
        this.changeDetector.markForCheck();
        try {
            const saveData = merge(this.activeTranslations(), this.draft());
            const obs = scope.isGlobal
                ? this.api.saveGlobalTranslations(saveData)
                : this.api.saveTypeTranslations(scopeId, saveData);
            await firstValueFrom(obs);

            this.savedTranslations.update((data) => {
                data[scopeId] = saveData;
                return structuredClone(data);
            });
            this.draft.set({});

            this.notifications.show({
                type: 'success',
                message: 'tool.save_success',
                translationParams: { count: changeCount },
            });
            /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
        } catch (err) {
            this.notifications.show({ type: 'alert', message: 'tool.save_error' });

            console.error('save failed', err);
        } finally {
            this.saving.set(false);
            this.changeDetector.markForCheck();
        }
    }

    async onDiscard(): Promise<void> {
        if (!(await this.askDiscard())) {
            return;
        }
        this.draft.set({});
        this.notifications.show({ type: 'default', message: 'tool.discard_success' });
        /* Pull-based ToolApi: nothing to push — the UI pulls hasUnsavedChanges() when it cares. */
    }
}

/* =====================================================================
 *  Pure utilities (kept at module level — no side effects)
 * ===================================================================== */

function merge(base: FormTranslations, delta: FormTranslations): FormTranslations {
    const result: FormTranslations = { ...base };
    for (const [key, langs] of Object.entries(delta)) {
        result[key] = { ...(result[key] ?? {}), ...langs };
    }
    return result;
}
