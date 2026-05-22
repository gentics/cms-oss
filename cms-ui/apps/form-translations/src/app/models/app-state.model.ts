/**
 * Generischer LoadingState für jede asynchrone Operation in der App.
 * UI-Komponenten verzweigen darauf statt auf einzelne booleans.
 */
export type LoadingState = 'idle' | 'loading' | 'loaded' | 'error';

/**
 * Persistierte UI-Settings (localStorage). Wird vom UserSettingsService verwaltet.
 */
export interface UserSettings {
  /** Zuletzt aktiver Scope (global oder Formulartyp-Key). */
  lastActiveScope?: string;
  /** Letzter Suchbegriff (optional, hilft nach Reload). */
  lastSearchTerm?: string;
  /** Letzter aktiver Vollständigkeits-Filter. */
  lastFilter?: 'all' | 'incomplete';
}

export const DEFAULT_USER_SETTINGS: UserSettings = {
  lastActiveScope: 'global',
  lastSearchTerm: '',
  lastFilter: 'all'
};
