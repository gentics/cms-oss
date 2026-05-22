import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subscription, combineLatest, map } from 'rxjs';

import { AppService } from '../../core/services/app.service';
import { AuthenticationService } from '../../core/services/authentication.service';
import { TranslationService } from '../../shared/translation.service';
import { LoadingState } from '../../models/app-state.model';
import { Language, Scope, ScopeData, ScopeId } from '../../models/translations.model';
import {
  FilterMode,
  FormTranslationsService
} from '../../services/form-translations.service';
import { CellEditEvent } from '../translations-table/translations-table.component';
import { ScopeTabInfo } from '../scope-tabs/scope-tabs.component';

interface ShellViewModel {
  ready: boolean;
  hasSession: boolean;
  sid: string | null;
  scopeTabs: ScopeTabInfo[];
  activeScope: Scope | null;
  activeScopeId: ScopeId;
  activeScopeData: ScopeData;
  languages: Language[];
  visibleKeys: string[];
  totalKeys: number;
  scopeLoading: LoadingState;
  scopeSaving: LoadingState;
  search: string;
  filter: FilterMode;
  dirtyCount: number;
}

@Component({
  selector: 'gtx-shell',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss']
})
export class ShellComponent implements OnInit, OnDestroy {
  readonly vm$: Observable<ShellViewModel>;
  private readonly subs = new Subscription();

  constructor(
    private readonly appService: AppService,
    private readonly auth: AuthenticationService,
    private readonly translations: FormTranslationsService,
    private readonly i18n: TranslationService
  ) {
    this.vm$ = combineLatest([
      this.appService.init,
      this.auth.sid,
      this.translations.state
    ]).pipe(
      map(([init, sid, state]) => {
        const totalKeys = collectAllKeys(state);
        const activeScope = state.scopes.find(s => s.id === state.activeScopeId) ?? null;
        const activeScopeData = state.data[state.activeScopeId] ?? {
          loading: 'idle' as const,
          saving: 'idle' as const,
          saved: {},
          draft: {}
        };
        const visibleKeys = filterKeys(totalKeys, state.search, state.filter, state.languages, activeScopeData);
        const scopeTabs: ScopeTabInfo[] = state.scopes.map(scope => {
          const data = state.data[scope.id];
          return {
            scope,
            translatedCount: countTranslated(data),
            totalCount: (totalKeys.length || (data ? Object.keys(data.saved).length : 0)) * state.languages.length,
            hasDirty: data ? this.translations.hasDirtyInScope(scope.id) : false
          };
        });
        return {
          ready: init.ready,
          hasSession: init.sessionEstablished,
          sid,
          scopeTabs,
          activeScope,
          activeScopeId: state.activeScopeId,
          activeScopeData,
          languages: state.languages,
          visibleKeys,
          totalKeys: totalKeys.length,
          scopeLoading: activeScopeData.loading,
          scopeSaving: activeScopeData.saving,
          search: state.search,
          filter: state.filter,
          dirtyCount: countAllDirty(state.data)
        };
      })
    );
  }

  ngOnInit(): void {
    /* Translations laden, sobald die App ready ist und eine Session besteht. */
    this.subs.add(
      this.appService.init.subscribe(init => {
        if (init.ready && init.sessionEstablished) {
          void this.translations.initialize();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  /* ---------- UI-Aktionen ---------- */

  onScopeSelect(scopeId: ScopeId): void {
    if (this.translations.hasDirtyInScope(this.translations.snapshot.activeScopeId)) {
      const confirmed = window.confirm(this.i18n.translate('SAVE_BAR.CONFIRM_SCOPE_SWITCH'));
      if (!confirmed) return;
      this.translations.discardActiveScope();
    }
    this.translations.setActiveScope(scopeId);
  }

  onSearchChange(value: string): void {
    this.translations.setSearch(value);
  }

  onFilterChange(filter: FilterMode): void {
    this.translations.setFilter(filter);
  }

  onCellEdit(event: CellEditEvent): void {
    this.translations.setCellValue(
      this.translations.snapshot.activeScopeId,
      event.key,
      event.langCode,
      event.value
    );
  }

  onSave(): void {
    void this.translations.saveActiveScope();
  }

  onDiscard(): void {
    if (!window.confirm(this.i18n.translate('SAVE_BAR.CONFIRM_DISCARD'))) return;
    this.translations.discardActiveScope();
  }
}

/* ---------- View-Helpers (lokal — Service hat eigene Helper, hier nur das, was die Shell braucht) ---------- */

function collectAllKeys(state: { data: Record<string, ScopeData> }): string[] {
  const set = new Set<string>();
  for (const data of Object.values(state.data)) {
    for (const key of Object.keys(data.saved)) set.add(key);
    for (const key of Object.keys(data.draft)) set.add(key);
  }
  return [...set].sort((a, b) => a.localeCompare(b));
}

function filterKeys(
  keys: string[],
  search: string,
  filter: FilterMode,
  languages: Language[],
  scopeData: ScopeData
): string[] {
  const q = search.trim().toLowerCase();
  return keys.filter(key => {
    if (q && !key.toLowerCase().includes(q)) return false;
    if (filter === 'incomplete') {
      const incomplete = languages.some(l =>
        (scopeData.draft[key]?.[l.code] ?? '').trim() === ''
      );
      if (!incomplete) return false;
    }
    return true;
  });
}

function countTranslated(data: ScopeData | undefined): number {
  if (!data) return 0;
  let n = 0;
  for (const row of Object.values(data.draft)) {
    for (const val of Object.values(row)) if (val.trim() !== '') n++;
  }
  return n;
}

function countAllDirty(data: Record<string, ScopeData>): number {
  let n = 0;
  for (const scope of Object.values(data)) {
    const keys = new Set([...Object.keys(scope.saved), ...Object.keys(scope.draft)]);
    for (const key of keys) {
      const a = scope.saved[key] ?? {};
      const b = scope.draft[key] ?? {};
      const langs = new Set([...Object.keys(a), ...Object.keys(b)]);
      for (const lang of langs) {
        if ((a[lang] ?? '') !== (b[lang] ?? '')) n++;
      }
    }
  }
  return n;
}
