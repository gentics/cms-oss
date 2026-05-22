import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  BehaviorSubject,
  Observable,
  combineLatest,
  firstValueFrom,
  map
} from 'rxjs';

import { environment } from '../../environments/environment';
import { ToolApiService } from '../core/services/tool-api.service';
import { NotificationService } from '../core/services/notification.service';
import { UserSettingsService } from '../core/services/user-settings.service';
import { TranslationService } from '../shared/translation.service';
import { LoadingState } from '../models/app-state.model';
import {
  FormTypeDto,
  FormTypesResponseDto,
  LanguageDto,
  LanguagesResponseDto,
  TranslationsPayloadDto,
  TranslationsResponseDto
} from '../models/dto.model';
import {
  GLOBAL_SCOPE_ID,
  Language,
  Scope,
  ScopeData,
  ScopeId,
  TranslationsMap,
  createEmptyScopeData
} from '../models/translations.model';

export type FilterMode = 'all' | 'incomplete';

export interface FormTranslationsState {
  languages: Language[];
  languagesLoading: LoadingState;
  scopes: Scope[];
  scopesLoading: LoadingState;
  activeScopeId: ScopeId;
  search: string;
  filter: FilterMode;
  /** Scope-Datenmap: scopeId → ScopeData (saved/draft/loading/saving). */
  data: Record<string, ScopeData>;
}

const INITIAL_STATE: FormTranslationsState = {
  languages: [],
  languagesLoading: 'idle',
  scopes: [],
  scopesLoading: 'idle',
  activeScopeId: GLOBAL_SCOPE_ID,
  search: '',
  filter: 'all',
  data: {}
};

@Injectable({ providedIn: 'root' })
export class FormTranslationsService {
  private readonly state$ = new BehaviorSubject<FormTranslationsState>(INITIAL_STATE);

  constructor(
    private readonly http: HttpClient,
    private readonly notifications: NotificationService,
    private readonly toolApi: ToolApiService,
    private readonly settings: UserSettingsService,
    private readonly i18n: TranslationService
  ) {}

  /* ---------- State-Exposition ---------- */

  get state(): Observable<FormTranslationsState> { return this.state$.asObservable(); }
  get snapshot(): FormTranslationsState { return this.state$.getValue(); }

  /** Liste aller Placeholder-Keys über alle Scopes hinweg, alphabetisch sortiert. */
  get placeholderKeys$(): Observable<string[]> {
    return this.state$.pipe(map(s => computePlaceholderKeys(s)));
  }

  /** Vollständig aufgelöster, anzuzeigender View. */
  get view$(): Observable<{
    keys: string[];
    activeScope: Scope | null;
    languages: Language[];
    scopeData: ScopeData;
    counts: { total: number; shown: number; dirty: number };
  }> {
    return combineLatest([this.state$]).pipe(
      map(([s]) => {
        const keys = computePlaceholderKeys(s);
        const activeScope = s.scopes.find(sc => sc.id === s.activeScopeId) ?? null;
        const scopeData = s.data[s.activeScopeId] ?? createEmptyScopeData();
        const shownKeys = filterKeys(keys, s, scopeData);
        return {
          keys: shownKeys,
          activeScope,
          languages: s.languages,
          scopeData,
          counts: {
            total: keys.length,
            shown: shownKeys.length,
            dirty: countDirty(s.data)
          }
        };
      })
    );
  }

  /* ---------- Bootstrap ---------- */

  /**
   * Lädt Sprachen + Formulartypen, baut die Scope-Liste auf und lädt die
   * Übersetzungen für den initial aktiven Scope.
   */
  async initialize(): Promise<void> {
    await Promise.all([this.loadLanguages(), this.loadFormTypes()]);

    const lastScope = this.settings.get('lastActiveScope') ?? GLOBAL_SCOPE_ID;
    const exists = this.snapshot.scopes.some(s => s.id === lastScope);
    const activeScopeId = exists ? lastScope : GLOBAL_SCOPE_ID;

    this.patch({
      activeScopeId,
      search: this.settings.get('lastSearchTerm') ?? '',
      filter: this.settings.get('lastFilter') ?? 'all'
    });

    await this.loadScope(activeScopeId);
  }

  /* ---------- Laden ---------- */

  private async loadLanguages(): Promise<void> {
    this.patch({ languagesLoading: 'loading' });
    try {
      const dto = await firstValueFrom(
        this.http.get<LanguagesResponseDto>(`${environment.apiBaseUrl}/form/translations/languages`)
      );
      this.patch({
        languages: normalizeLanguages(dto),
        languagesLoading: 'loaded'
      });
    } catch {
      this.patch({ languagesLoading: 'error' });
      this.notifications.error(this.i18n.translate('NOTIFY.LANGUAGES_LOAD_ERROR'));
    }
  }

  private async loadFormTypes(): Promise<void> {
    this.patch({ scopesLoading: 'loading' });
    try {
      const dto = await firstValueFrom(
        this.http.get<FormTypesResponseDto>(`${environment.apiBaseUrl}/form/types`)
      );
      const types = normalizeFormTypes(dto);
      const scopes: Scope[] = [
        {
          id: GLOBAL_SCOPE_ID,
          label: this.i18n.translate('SCOPE.GLOBAL_TITLE'),
          description: this.i18n.translate('SCOPE.GLOBAL_DESC'),
          isGlobal: true
        },
        ...types.map(t => ({
          id: t.key,
          label: t.name ?? t.key,
          description: this.i18n.translate('SCOPE.TYPE_DESC', { name: t.name ?? t.key }),
          isGlobal: false
        }))
      ];
      this.patch({ scopes, scopesLoading: 'loaded' });
    } catch {
      this.patch({
        /* Fallback: zumindest Global anbieten */
        scopes: [{
          id: GLOBAL_SCOPE_ID,
          label: this.i18n.translate('SCOPE.GLOBAL_TITLE'),
          description: this.i18n.translate('SCOPE.GLOBAL_DESC'),
          isGlobal: true
        }],
        scopesLoading: 'error'
      });
      this.notifications.error(this.i18n.translate('NOTIFY.FORM_TYPES_LOAD_ERROR'));
    }
  }

  async loadScope(scopeId: ScopeId): Promise<void> {
    const scope = this.snapshot.scopes.find(s => s.id === scopeId);
    if (!scope) return;

    const existing = this.snapshot.data[scopeId];
    if (existing && existing.loading === 'loaded') return;

    this.updateScopeData(scopeId, prev => ({ ...prev, loading: 'loading' }));

    const url = scope.isGlobal
      ? `${environment.apiBaseUrl}/form/translations`
      : `${environment.apiBaseUrl}/form/types/${encodeURIComponent(scopeId)}/translations`;

    try {
      const dto = await firstValueFrom(this.http.get<TranslationsResponseDto>(url));
      const payload = extractTranslations(dto);
      this.updateScopeData(scopeId, () => ({
        loading: 'loaded',
        saving: 'idle',
        saved: payload,
        draft: deepClone(payload)
      }));
    } catch {
      this.updateScopeData(scopeId, prev => ({ ...prev, loading: 'error' }));
      this.notifications.error(this.i18n.translate('NOTIFY.SCOPE_LOAD_ERROR', { scope: scope.label }));
    }
  }

  /* ---------- Edit-Aktionen ---------- */

  setActiveScope(scopeId: ScopeId): void {
    if (scopeId === this.snapshot.activeScopeId) return;
    this.patch({ activeScopeId: scopeId });
    this.settings.set('lastActiveScope', scopeId);
    /* lazy load der Scope-Daten */
    void this.loadScope(scopeId);
  }

  setSearch(term: string): void {
    this.patch({ search: term });
    this.settings.set('lastSearchTerm', term);
  }

  setFilter(filter: FilterMode): void {
    this.patch({ filter });
    this.settings.set('lastFilter', filter);
  }

  setCellValue(scopeId: ScopeId, key: string, lang: string, value: string): void {
    this.updateScopeData(scopeId, prev => {
      const nextDraft: TranslationsMap = { ...prev.draft };
      const row = { ...(nextDraft[key] ?? {}) };
      row[lang] = value;
      nextDraft[key] = row;
      return { ...prev, draft: nextDraft };
    });
    this.toolApi.setUnsavedChanges(this.hasAnyDirty());
  }

  /* ---------- Speichern / Verwerfen ---------- */

  /**
   * Speichert die Änderungen des aktuell aktiven Scopes via POST.
   * Sendet nur das Diff (nur Keys/Sprachen, die sich gegenüber `saved` geändert haben).
   */
  async saveActiveScope(): Promise<void> {
    const scopeId = this.snapshot.activeScopeId;
    const scope = this.snapshot.scopes.find(s => s.id === scopeId);
    const data = this.snapshot.data[scopeId];
    if (!scope || !data) return;

    const diff = diffTranslations(data.saved, data.draft);
    const changedCells = countCells(diff);
    if (changedCells === 0) return;

    this.updateScopeData(scopeId, prev => ({ ...prev, saving: 'loading' }));

    const url = scope.isGlobal
      ? `${environment.apiBaseUrl}/form/translations`
      : `${environment.apiBaseUrl}/form/types/${encodeURIComponent(scopeId)}/translations`;

    try {
      await firstValueFrom(this.http.post<TranslationsResponseDto>(url, diff));
      const newSaved = mergeTranslations(data.saved, diff);
      this.updateScopeData(scopeId, () => ({
        loading: 'loaded',
        saving: 'idle',
        saved: newSaved,
        draft: deepClone(newSaved)
      }));
      this.notifications.success(
        this.i18n.translate('NOTIFY.SAVE_SUCCESS', { count: changedCells })
      );
      this.toolApi.setUnsavedChanges(this.hasAnyDirty());
    } catch {
      this.updateScopeData(scopeId, prev => ({ ...prev, saving: 'error' }));
      this.notifications.error(this.i18n.translate('NOTIFY.SAVE_ERROR'));
    }
  }

  /** Verwirft Änderungen im aktiven Scope. */
  discardActiveScope(): void {
    const scopeId = this.snapshot.activeScopeId;
    this.updateScopeData(scopeId, prev => ({
      ...prev,
      draft: deepClone(prev.saved)
    }));
    this.toolApi.setUnsavedChanges(this.hasAnyDirty());
    this.notifications.info(this.i18n.translate('NOTIFY.DISCARD_DONE'));
  }

  /* ---------- Helpers ---------- */

  hasDirtyInScope(scopeId: ScopeId): boolean {
    const data = this.snapshot.data[scopeId];
    if (!data) return false;
    return countCells(diffTranslations(data.saved, data.draft)) > 0;
  }

  hasAnyDirty(): boolean {
    return Object.keys(this.snapshot.data).some(id => this.hasDirtyInScope(id));
  }

  private patch(partial: Partial<FormTranslationsState>): void {
    this.state$.next({ ...this.state$.getValue(), ...partial });
  }

  private updateScopeData(scopeId: ScopeId, updater: (prev: ScopeData) => ScopeData): void {
    const current = this.snapshot;
    const prev = current.data[scopeId] ?? createEmptyScopeData();
    const next = updater(prev);
    this.patch({ data: { ...current.data, [scopeId]: next } });
  }
}

/* =====================================================================
   Pure Helpers
   ===================================================================== */

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function normalizeLanguages(dto: LanguagesResponseDto): Language[] {
  const raw: (LanguageDto | string)[] = Array.isArray(dto)
    ? (dto as (LanguageDto | string)[])
    : ((dto.languages ?? []) as (LanguageDto | string)[]);
  return raw.map(item => {
    if (typeof item === 'string') return { code: item, name: item.toUpperCase() };
    return { code: item.code, name: item.name && item.name.trim() !== '' ? item.name : item.code.toUpperCase() };
  });
}

function normalizeFormTypes(dto: FormTypesResponseDto): FormTypeDto[] {
  const raw: (FormTypeDto | string)[] = Array.isArray(dto)
    ? (dto as (FormTypeDto | string)[])
    : ((dto.types ?? []) as (FormTypeDto | string)[]);
  return raw.map(item => {
    if (typeof item === 'string') return { key: item, name: item };
    return { key: item.key, name: item.name ?? item.key, description: item.description };
  });
}

function extractTranslations(dto: TranslationsResponseDto): TranslationsPayloadDto {
  /* Wrapper-Form: { responseInfo, translations: ... } */
  if (dto && typeof dto === 'object' && 'translations' in (dto as Record<string, unknown>)) {
    return ((dto as { translations: TranslationsPayloadDto }).translations) ?? {};
  }
  /* Raw-Form: direkt die Map */
  return (dto as TranslationsPayloadDto) ?? {};
}

function computePlaceholderKeys(state: FormTranslationsState): string[] {
  const set = new Set<string>();
  for (const data of Object.values(state.data)) {
    for (const key of Object.keys(data.saved)) set.add(key);
    for (const key of Object.keys(data.draft)) set.add(key);
  }
  return [...set].sort((a, b) => a.localeCompare(b));
}

function filterKeys(
  keys: string[],
  state: FormTranslationsState,
  scopeData: ScopeData
): string[] {
  const q = state.search.trim().toLowerCase();
  return keys.filter(key => {
    if (q && !key.toLowerCase().includes(q)) return false;
    if (state.filter === 'incomplete') {
      const incomplete = state.languages.some(l =>
        ((scopeData.draft[key]?.[l.code]) ?? '').trim() === ''
      );
      if (!incomplete) return false;
    }
    return true;
  });
}

function diffTranslations(
  saved: TranslationsMap,
  draft: TranslationsMap
): TranslationsPayloadDto {
  const result: TranslationsPayloadDto = {};
  const keys = new Set([...Object.keys(saved), ...Object.keys(draft)]);
  for (const key of keys) {
    const a = saved[key] ?? {};
    const b = draft[key] ?? {};
    const langs = new Set([...Object.keys(a), ...Object.keys(b)]);
    const changedLangs: Record<string, string> = {};
    for (const lang of langs) {
      const av = a[lang] ?? '';
      const bv = b[lang] ?? '';
      if (av !== bv) changedLangs[lang] = bv;
    }
    if (Object.keys(changedLangs).length > 0) result[key] = changedLangs;
  }
  return result;
}

function mergeTranslations(
  base: TranslationsMap,
  delta: TranslationsPayloadDto
): TranslationsMap {
  const result: TranslationsMap = { ...base };
  for (const [key, langs] of Object.entries(delta)) {
    result[key] = { ...(result[key] ?? {}), ...langs };
  }
  return result;
}

function countCells(map: TranslationsPayloadDto): number {
  let n = 0;
  for (const langs of Object.values(map)) n += Object.keys(langs).length;
  return n;
}

function countDirty(data: Record<string, ScopeData>): number {
  let n = 0;
  for (const scope of Object.values(data)) {
    n += countCells(diffTranslations(scope.saved, scope.draft));
  }
  return n;
}
