import { Injectable } from '@angular/core';

import { DEFAULT_USER_SETTINGS, UserSettings } from '../../models/app-state.model';

const STORAGE_KEY = 'gtx-form-translations.user-settings';

/**
 * Persistiert UI-Einstellungen in localStorage. Niemals sensitive Daten speichern.
 */
@Injectable({ providedIn: 'root' })
export class UserSettingsService {
  private settings: UserSettings = { ...DEFAULT_USER_SETTINGS };

  constructor() {
    this.load();
  }

  get<K extends keyof UserSettings>(key: K): UserSettings[K] | undefined {
    return this.settings[key];
  }

  set<K extends keyof UserSettings>(key: K, value: UserSettings[K]): void {
    this.settings = { ...this.settings, [key]: value };
    this.persist();
  }

  patch(partial: Partial<UserSettings>): void {
    this.settings = { ...this.settings, ...partial };
    this.persist();
  }

  reset(): void {
    this.settings = { ...DEFAULT_USER_SETTINGS };
    this.persist();
  }

  private load(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<UserSettings>;
        this.settings = { ...DEFAULT_USER_SETTINGS, ...parsed };
      }
    } catch {
      this.settings = { ...DEFAULT_USER_SETTINGS };
    }
  }

  private persist(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
    } catch { /* localStorage kann blockiert sein, kein Hard-Error */ }
  }
}
