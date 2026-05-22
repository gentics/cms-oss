import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { TranslationService } from '../../shared/translation.service';
import { AuthenticationService } from './authentication.service';
import { ToolApiService } from './tool-api.service';

export interface AppInitState {
  ready: boolean;
  sessionEstablished: boolean;
  /** Fehlerursache, wenn !ready. */
  error?: 'no-session' | 'unknown';
}

/**
 * Orchestriert die Bootstrap-Sequenz:
 *   1. Auth-SID etablieren (URL → sessionStorage → Mock)
 *   2. ToolAPI-Handshake mit der CMS-Shell starten
 *   3. UI-Sprache laden
 * Erst wenn alles bereit ist, wird `init$.ready = true` gemeldet.
 */
@Injectable({ providedIn: 'root' })
export class AppService {
  private readonly init$ = new BehaviorSubject<AppInitState>({
    ready: false,
    sessionEstablished: false
  });

  constructor(
    private readonly auth: AuthenticationService,
    private readonly toolApi: ToolApiService,
    private readonly translations: TranslationService
  ) {}

  get init(): Observable<AppInitState> {
    return this.init$.asObservable();
  }

  async initialize(): Promise<void> {
    const sessionOk = this.auth.initialize();
    if (!sessionOk) {
      this.init$.next({ ready: true, sessionEstablished: false, error: 'no-session' });
      return;
    }

    this.toolApi.initialize();
    await this.translations.loadInitialLocale();

    this.init$.next({ ready: true, sessionEstablished: true });
  }
}
