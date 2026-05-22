import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output
} from '@angular/core';

import { Language, ScopeData } from '../../models/translations.model';

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
  styleUrls: ['./translations-table.component.scss']
})
export class TranslationsTableComponent {
  @Input() keys: string[] = [];
  @Input() languages: Language[] = [];
  @Input({ required: true }) scopeData!: ScopeData;

  @Output() readonly cellEdit = new EventEmitter<CellEditEvent>();

  getValue(key: string, langCode: string): string {
    return this.scopeData.draft[key]?.[langCode] ?? '';
  }

  getCellState(key: string, langCode: string): CellState {
    const draft = this.scopeData.draft[key]?.[langCode] ?? '';
    const saved = this.scopeData.saved[key]?.[langCode] ?? '';
    if (draft !== saved) return 'dirty';
    if (draft.trim() === '') return 'empty';
    return 'saved';
  }

  onInput(key: string, langCode: string, event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.cellEdit.emit({ key, langCode, value });
  }

  trackKey(_idx: number, key: string): string { return key; }
  trackLang(_idx: number, lang: Language): string { return lang.code; }
}
