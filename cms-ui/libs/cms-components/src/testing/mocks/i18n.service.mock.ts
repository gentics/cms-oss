import { NEVER, Observable, of } from 'rxjs';
import { FALLBACK_LANGUAGE, JoinOptions, TranslateParameters } from '../../lib/common/models';
import { I18nService } from '../../lib/core/providers/i18n/i18n.service';

export class MockI18nService implements Required<I18nService> {

    private lang = FALLBACK_LANGUAGE;
    private languages = new Set([FALLBACK_LANGUAGE]);

    public getCurrentLanguage(): string {
        return this.lang;
    }

    public getAvailableLangues(): readonly string[] {
        return Array.from(this.languages);
    }

    public setLanguage(language: string): void {
        this.lang = language;
        this.languages.add(language);
    }

    public inferUserLanguage(): string {
        return navigator.language;
    }

    public join(parts: string[], _options?: JoinOptions): string {
        return parts.join(',');
    }

    public applyShortcuts(value: string, _params?: TranslateParameters): string {
        return value;
    }

    public translateParamsInstant(params: TranslateParameters): TranslateParameters {
        return params;
    }

    public onLanguageChange(): Observable<string> {
        return NEVER;
    }

    public instant(key: string | string[], _params?: TranslateParameters): string {
        if (Array.isArray(key)) {
            return key.join('-');
        }
        return key;
    }

    public get(key: string | string[], _params?: TranslateParameters): Observable<string> {
        return of(Array.isArray(key) ? key.join('-') : key);
    }
}
