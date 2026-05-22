import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

const STORAGE_KEY = 'gtx-form-translations.sid';

/**
 * Hält die aktuelle CMS-Session-ID. Wird beim Bootstrap durch den AppService
 * via `initialize()` befüllt. Quellen, in Reihenfolge der Priorität:
 *   1. URL-Parameter `?sid=…` (Production-Pfad — CMS embedded das Tool so)
 *   2. sessionStorage (Fallback nach Reload)
 *   3. environment.mockSid (nur im Mock-Modus)
 */
@Injectable({ providedIn: 'root' })
export class AuthenticationService {
  private readonly sid$ = new BehaviorSubject<string | null>(null);

  /** Initialisiert die SID. Gibt true zurück, wenn eine SID etabliert werden konnte. */
  initialize(): boolean {
    const fromUrl = this.extractSidFromUrl();
    if (fromUrl) {
      this.setSid(fromUrl);
      return true;
    }
    const fromStorage = this.loadSidFromStorage();
    if (fromStorage) {
      this.setSid(fromStorage);
      return true;
    }
    if (environment.useMockData && environment.mockSid) {
      this.setSid(environment.mockSid);
      return true;
    }
    return false;
  }

  get sid(): Observable<string | null> {
    return this.sid$.asObservable();
  }

  get currentSid(): string | null {
    return this.sid$.getValue();
  }

  setSid(sid: string): void {
    this.sid$.next(sid);
    try {
      sessionStorage.setItem(STORAGE_KEY, sid);
    } catch {
      /* sessionStorage kann blockiert sein, kein Hard-Error */
    }
  }

  clearSid(): void {
    this.sid$.next(null);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch { /* ignore */ }
  }

  private extractSidFromUrl(): string | null {
    try {
      const params = new URLSearchParams(window.location.search);
      const sid = params.get('sid');
      return sid && sid.trim() !== '' ? sid : null;
    } catch {
      return null;
    }
  }

  private loadSidFromStorage(): string | null {
    try {
      return sessionStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }
}
