import { ChangeDetectorRef, Injectable, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Subscription } from 'rxjs';
import { I18nService } from '../../providers/i18n/i18n.service';

/**
 * Resolves a per-language object (`{ de: 'Wert', en: 'Value' }`) to the string
 * for the currently active UI language.
 *
 * Useful for configuration values that come from outside the editor-ui bundle
 * (e.g. dropped into a customer config file) and therefore cannot use the
 * regular `gtxI18n` pipe which relies on bundled translation keys.
 *
 * Usage:
 * ```html
 * {{ action.labelI18n | gtxI18nObject }}
 * ```
 *
 * Resolution rules — in order:
 * 1. exact match on the active UI language (e.g. `de`)
 * 2. fallback chain configured for the pipe instance (defaults to `['en']`)
 * 3. first available value in the object
 * 4. empty string
 *
 * Pure-false because it must re-evaluate when the UI language changes
 * without the underlying input reference changing.
 */
@Injectable()
@Pipe({
    name: 'gtxI18nObject',
    pure: false,
    standalone: false,
})
export class I18nObjectPipe implements PipeTransform, OnDestroy {

    private subscription: Subscription;
    private lastInput: Record<string, string> | null | undefined = undefined;
    private lastFallback: readonly string[] | undefined = undefined;
    private lastResult = '';
    private currentLang: string;

    constructor(
        private i18n: I18nService,
        private changeDetector: ChangeDetectorRef,
    ) {
        this.currentLang = this.i18n.getCurrentLanguage();
        this.subscription = this.i18n.onLanguageChange().subscribe((lang) => {
            this.currentLang = lang;
            // Force re-evaluation on next change-detection.
            this.lastInput = undefined;
            this.changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.subscription?.unsubscribe();
    }

    transform(input: Record<string, string> | null | undefined, fallback: readonly string[] = ['en']): string {
        // Cheap memoisation — pure-false pipes are evaluated every CD cycle.
        if (input === this.lastInput && fallback === this.lastFallback) {
            return this.lastResult;
        }
        this.lastInput = input;
        this.lastFallback = fallback;

        if (input == null || typeof input !== 'object') {
            this.lastResult = '';
            return this.lastResult;
        }

        // 1) exact match on active UI language
        if (typeof input[this.currentLang] === 'string') {
            this.lastResult = input[this.currentLang];
            return this.lastResult;
        }

        // 2) configured fallbacks (default ['en'])
        for (const code of fallback) {
            if (typeof input[code] === 'string') {
                this.lastResult = input[code];
                return this.lastResult;
            }
        }

        // 3) first available value in the object
        for (const key of Object.keys(input)) {
            if (typeof input[key] === 'string') {
                this.lastResult = input[key];
                return this.lastResult;
            }
        }

        // 4) nothing to show
        this.lastResult = '';
        return this.lastResult;
    }
}
