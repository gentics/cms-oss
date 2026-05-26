import { Injectable, inject } from '@angular/core';
import {
    FormTranslations,
    FormTranslationsLanguage,
    FormTypeConfiguration,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Observable, map } from 'rxjs';

/**
 * Thin domain wrapper around the form-translation endpoints exposed by
 * `GCMSRestClientService`. Holds no state — callers (typically `ShellComponent`)
 * own the UI state directly.
 */
@Injectable({ providedIn: 'root' })
export class FormTranslationsApiService {

    private readonly client = inject(GCMSRestClientService);

    loadLanguages(): Observable<FormTranslationsLanguage[]> {
        return this.client.form.listTranslationLanguages().pipe(
            map(res => normaliseLanguages(res.languages)),
        );
    }

    loadFormTypes(): Observable<FormTypeConfiguration[]> {
        return this.client.form.listConfigurations().pipe(
            map(res => res.items ?? []),
        );
    }

    loadGlobalTranslations(): Observable<FormTranslations> {
        return this.client.form.listTranslations().pipe(
            map(res => res.translations ?? {}),
        );
    }

    loadTypeTranslations(formType: string): Observable<FormTranslations> {
        return this.client.form.listTypeTranslations(formType).pipe(
            map(res => res.translations ?? {}),
        );
    }

    saveGlobalTranslations(diff: FormTranslations): Observable<FormTranslations> {
        return this.client.form.updateTranslations(diff).pipe(
            map(res => res.translations ?? {}),
        );
    }

    saveTypeTranslations(formType: string, diff: FormTranslations): Observable<FormTranslations> {
        return this.client.form.updateTypeTranslations(formType, diff).pipe(
            map(res => res.translations ?? {}),
        );
    }
}

function normaliseLanguages(raw: FormTranslationsLanguage[] | string[] | undefined): FormTranslationsLanguage[] {
    if (!raw) return [];
    return raw.map(item => {
        if (typeof item === 'string') return { code: item, name: item.toUpperCase() };
        return { code: item.code, name: item.name && item.name.trim() !== '' ? item.name : item.code.toUpperCase() };
    });
}
