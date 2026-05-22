import { Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Nachrichten Tool → CMS-Shell.
 */
type OutgoingMessage =
  | { type: 'TOOL_READY' }
  | { type: 'HAS_UNSAVED_CHANGES'; payload: { hasUnsavedChanges: boolean } }
  | { type: 'SAVE_STATE'; payload: unknown }
  | { type: 'RESTORE_STATE' };

/**
 * Nachrichten CMS-Shell → Tool.
 */
export type IncomingMessage =
  | { type: 'HANDSHAKE_CONFIRM'; payload?: unknown }
  | { type: 'STATE_RESTORED'; payload?: unknown }
  | { type: 'NAVIGATE'; payload: { target?: string } };

type ConnectionMode = 'connecting' | 'connected' | 'standalone';

/**
 * PostMessage-Brücke zur CMS-Shell. Im Standalone-Modus (kein Parent-Frame)
 * werden alle Outbound-Messages still verworfen — das Tool funktioniert
 * unverändert in der lokalen Entwicklung.
 */
@Injectable({ providedIn: 'root' })
export class ToolApiService {
  private readonly mode$ = new BehaviorSubject<ConnectionMode>('connecting');
  private readonly incoming$ = new Subject<IncomingMessage>();
  private port: MessagePort | null = null;
  private lastUnsavedChanges: boolean | null = null;

  constructor(private readonly ngZone: NgZone) {}

  get mode(): Observable<ConnectionMode> { return this.mode$.asObservable(); }
  get incoming(): Observable<IncomingMessage> { return this.incoming$.asObservable(); }

  /**
   * Startet den Handshake mit der CMS-Shell. Wenn nach dem Timeout
   * keine Antwort kommt, wechselt der Service in den standalone-Modus.
   */
  initialize(): void {
    if (window.parent === window) {
      /* Keine iframe-Einbettung → standalone */
      this.mode$.next('standalone');
      return;
    }

    window.addEventListener('message', this.handleWindowMessage);

    const send = (): void => {
      try {
        window.parent.postMessage({ type: 'TOOL_READY', toolKey: environment.toolKey }, '*');
      } catch {
        /* postMessage kann u.U. fehlschlagen, dann bleiben wir connecting */
      }
    };

    let attempts = 0;
    const tryHandshake = (): void => {
      if (this.mode$.getValue() === 'connected') return;
      attempts++;
      send();
      if (attempts >= environment.toolApiHandshakeRetries) return;
      setTimeout(tryHandshake, environment.toolApiHandshakeTimeoutMs / environment.toolApiHandshakeRetries);
    };
    tryHandshake();

    /* Final-Timeout: wenn nach Gesamtzeit kein Confirm kam → standalone */
    setTimeout(() => {
      if (this.mode$.getValue() === 'connecting') {
        this.mode$.next('standalone');
      }
    }, environment.toolApiHandshakeTimeoutMs);
  }

  setUnsavedChanges(hasUnsavedChanges: boolean): void {
    /* De-dupe — sonst spammen wir die Shell bei jedem Tastendruck */
    if (this.lastUnsavedChanges === hasUnsavedChanges) return;
    this.lastUnsavedChanges = hasUnsavedChanges;
    this.send({ type: 'HAS_UNSAVED_CHANGES', payload: { hasUnsavedChanges } });
  }

  saveState(state: unknown): void {
    this.send({ type: 'SAVE_STATE', payload: state });
  }

  requestRestoreState(): void {
    this.send({ type: 'RESTORE_STATE' });
  }

  private send(message: OutgoingMessage): void {
    if (this.mode$.getValue() !== 'connected' || !this.port) return;
    try {
      this.port.postMessage(message);
    } catch { /* still verwerfen */ }
  }

  private readonly handleWindowMessage = (event: MessageEvent): void => {
    const data = event.data as { type?: string } | null;
    if (!data || typeof data.type !== 'string') return;

    if (data.type === 'HANDSHAKE_CONFIRM' && event.ports && event.ports.length > 0) {
      this.port = event.ports[0];
      this.port.onmessage = (msgEvent: MessageEvent) => {
        const msg = msgEvent.data as IncomingMessage;
        if (msg && typeof msg.type === 'string') {
          this.ngZone.run(() => this.incoming$.next(msg));
        }
      };
      this.ngZone.run(() => this.mode$.next('connected'));
    }
  };
}
