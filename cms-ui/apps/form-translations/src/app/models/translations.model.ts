import { LoadingState } from './app-state.model';

/**
 * ViewModels für die UI. Werden aus DTOs (siehe dto.model.ts) transformiert.
 */

export interface Language {
  code: string;
  name: string;
}

/** Sentinel-Key für den globalen Scope (gegenüber Formulartyp-Scopes). */
export const GLOBAL_SCOPE_ID = 'global';
export type ScopeId = typeof GLOBAL_SCOPE_ID | string;

export interface Scope {
  id: ScopeId;
  /** Anzeigename in den Tabs. */
  label: string;
  /** Beschreibungstext für die Scope-Karte. Darf HTML enthalten. */
  description: string;
  /**
   * true für den globalen Scope, false für Formulartyp-Scopes.
   * Wichtig u.a. für die Endpoint-Wahl beim Save.
   */
  isGlobal: boolean;
}

/**
 * Map: Placeholder-Key → Sprachcode → Übersetzungstext.
 * Identische Struktur wie das POST-Payload der API.
 */
export type TranslationsMap = Record<string, Record<string, string>>;

/**
 * Zustand pro Scope.
 * - `saved` spiegelt den letzten erfolgreich vom Server bestätigten Stand.
 * - `draft` enthält die aktuell im UI editierten Werte.
 * - Abweichung saved ↔ draft = dirty.
 */
export interface ScopeData {
  loading: LoadingState;
  saving: LoadingState;
  saved: TranslationsMap;
  draft: TranslationsMap;
}

export function createEmptyScopeData(): ScopeData {
  return {
    loading: 'idle',
    saving: 'idle',
    saved: {},
    draft: {}
  };
}
