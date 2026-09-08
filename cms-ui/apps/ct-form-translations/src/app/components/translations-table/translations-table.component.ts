import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';
import { FormTranslations, FormTranslationsLanguage } from '@gentics/cms-models';

export interface CellEditEvent {
    key: string;
    langCode: string;
    value: string;
}

type CellState = 'saved' | 'dirty' | 'empty';

@Component({
    selector: 'gtx-translations-table',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './translations-table.component.html',
    styleUrls: ['./translations-table.component.scss'],
})
export class TranslationsTableComponent {
    @Input() keys: string[] = [];
    @Input() languages: FormTranslationsLanguage[] = [];
    @Input() saved: FormTranslations = {};
    @Input() draft: FormTranslations = {};
    /**
     * Empty placeholders per language code, for the figure in the column header.
     *
     * Deliberately not derived from `keys`: those are already filtered by the
     * search, and a header number that shifts while typing is useless.
     */
    @Input() missingByLanguage: Record<string, number> = {};

    @Output() readonly cellEdit = new EventEmitter<CellEditEvent>();

    /** `name` is optional on the wire, so the code is the fallback. */
    displayName(lang: FormTranslationsLanguage): string {
        return lang.name || lang.code;
    }

    /** Header figure for one column, `null` while the data is still loading. */
    missingFor(langCode: string): number | null {
        return this.missingByLanguage[langCode] ?? null;
    }

    // TODO: The rows and cells should be pre-computed and define a state, rather
    // than loading the data in the template.
    getValue(key: string, langCode: string): string {
        if (this.draft[key] == null) {
            return this.saved[key]?.[langCode] ?? '';
        }
        if (this.draft[key]?.[langCode] == null) {
            return this.saved[key]?.[langCode] ?? '';
        }
        return this.draft[key][langCode];
    }

    getCellState(key: string, langCode: string): CellState {
        const savedValue = this.saved[key]?.[langCode] ?? '';
        if (this.draft[key] == null) {
            return savedValue.trim() === ''
                ? 'empty'
                : 'saved';
        }
        const draftValue = this.draft[key][langCode];
        if (draftValue == null) {
            return savedValue.trim() === ''
                ? 'empty'
                : 'saved';
        }
        if (draftValue !== savedValue) {
            return 'dirty';
        }
        if (draftValue.trim() === '') {
            return 'empty';
        }
        return 'saved';
    }

    onInput(key: string, langCode: string, event: Event): void {
        const value = (event.target as HTMLInputElement).value;
        this.cellEdit.emit({ key, langCode, value });
    }
}
