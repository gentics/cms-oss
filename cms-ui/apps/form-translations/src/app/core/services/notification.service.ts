import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ToastKind = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
  /** Timestamp (ms) wann der Toast automatisch entfernt werden soll. */
  expiresAt: number;
}

const DEFAULT_DURATION_MS = 4000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly toasts$ = new BehaviorSubject<Toast[]>([]);
  private nextId = 1;

  get toasts(): Observable<Toast[]> {
    return this.toasts$.asObservable();
  }

  success(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.push('success', message, durationMs);
  }
  error(message: string, durationMs = DEFAULT_DURATION_MS + 2000): void {
    this.push('error', message, durationMs);
  }
  warning(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.push('warning', message, durationMs);
  }
  info(message: string, durationMs = DEFAULT_DURATION_MS): void {
    this.push('info', message, durationMs);
  }

  dismiss(id: number): void {
    this.toasts$.next(this.toasts$.getValue().filter(t => t.id !== id));
  }

  private push(kind: ToastKind, message: string, durationMs: number): void {
    const toast: Toast = {
      id: this.nextId++,
      kind,
      message,
      expiresAt: Date.now() + durationMs
    };
    this.toasts$.next([...this.toasts$.getValue(), toast]);
    setTimeout(() => this.dismiss(toast.id), durationMs);
  }
}
