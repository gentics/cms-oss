import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, firstValueFrom } from 'rxjs';

type LocaleCode = 'de' | 'en';
type Dictionary = Record<string, string>;

const SUPPORTED: LocaleCode[] = ['de', 'en'];
const DEFAULT_LOCALE: LocaleCode = 'de';
const STORAGE_KEY = 'gtx-form-translations.locale';

/**
 * Lädt die UI-Texte des Tools (assets/i18n/<locale>.json). Komplett unabhängig
 * von den Form-Engine-Übersetzungen, die dieses Tool pflegt.
 */
@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly locale$ = new BehaviorSubject<LocaleCode>(DEFAULT_LOCALE);
  private dict: Dictionary = {};

  constructor(private readonly http: HttpClient) {}

  get locale(): Observable<LocaleCode> { return this.locale$.asObservable(); }
  get currentLocale(): LocaleCode { return this.locale$.getValue(); }

  async loadInitialLocale(): Promise<void> {
    const locale = this.detectLocale();
    await this.use(locale);
  }

  async use(locale: LocaleCode): Promise<void> {
    if (!SUPPORTED.includes(locale)) locale = DEFAULT_LOCALE;
    this.locale$.next(locale);
    try { localStorage.setItem(STORAGE_KEY, locale); } catch { /* ignore */ }
    try {
      this.dict = await firstValueFrom(
        this.http.get<Dictionary>(`assets/i18n/${locale}.json`)
      );
    } catch {
      this.dict = {};
    }
  }

  translate(key: string, params?: Record<string, string | number>): string {
    let value = this.dict[key] ?? key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        value = value.replace(new RegExp(`\\{\\{\\s*${k}\\s*\\}\\}`, 'g'), String(v));
      }
    }
    return value;
  }

  private detectLocale(): LocaleCode {
    try {
      const stored = localStorage.getItem(STORAGE_KEY) as LocaleCode | null;
      if (stored && SUPPORTED.includes(stored)) return stored;
    } catch { /* ignore */ }
    const nav = (navigator.language ?? '').slice(0, 2).toLowerCase() as LocaleCode;
    return SUPPORTED.includes(nav) ? nav : DEFAULT_LOCALE;
  }
}
