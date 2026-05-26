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

    @Output() readonly cellEdit = new EventEmitter<CellEditEvent>();

    getValue(key: string, langCode: string): string {
        return this.draft[key]?.[langCode] ?? '';
    }

    getCellState(key: string, langCode: string): CellState {
        const draftValue = this.draft[key]?.[langCode] ?? '';
        const savedValue = this.saved[key]?.[langCode] ?? '';
        if (draftValue !== savedValue) return 'dirty';
        if (draftValue.trim() === '') return 'empty';
        return 'saved';
    }

    onInput(key: string, langCode: string, event: Event): void {
        const value = (event.target as HTMLInputElement).value;
        this.cellEdit.emit({ key, langCode, value });
    }

    trackKey(_idx: number, key: string): string { return key; }
    trackLang(_idx: number, lang: FormTranslationsLanguage): string { return lang.code; }
}
