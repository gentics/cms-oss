import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';
import { FilterMode } from '../../models/translations.model';

@Component({
    selector: 'gtx-translations-toolbar',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './translations-toolbar.component.html',
    styleUrls: ['./translations-toolbar.component.scss'],
})
export class TranslationsToolbarComponent {
    @Input() search = '';
    @Input() filter: FilterMode = 'all';
    @Input() shown = 0;
    @Input() total = 0;

    @Output() readonly searchChange = new EventEmitter<string>();
    @Output() readonly filterChange = new EventEmitter<FilterMode>();

    onSearchInput(value: string): void {
        // The search-bar component is hacked together and events faulty events
        // from time to time. Only string events should be accepted.
        if (typeof value !== 'string') {
            return;
        }
        this.searchChange.emit(value);
    }

    onFilter(mode: FilterMode): void {
        if (mode !== this.filter) this.filterChange.emit(mode);
    }
}
