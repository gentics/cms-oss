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
