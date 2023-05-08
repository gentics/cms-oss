import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Language } from '@gentics/cms-models';

@Component({
    selector: 'gtxct-filter-translation',
    templateUrl: './filter-translation.component.html',
    styleUrls: ['./filter-translation.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class FilterTranslationComponent implements OnInit {
    @Input() activeLanguages: any;
    @Input() languages: Language[] = [];

    /** Fired when a node has been selected. */
    @Output() translationSelected = new EventEmitter<string>();

    constructor() { }

    ngOnInit(): void { }

    isActive(id: number): boolean {
        if (this.activeLanguages.includes(id)) {
            return true;
        } else {
            return false;
        }
    }

    select(translation: string): void {
        this.translationSelected.emit(translation);
    }
}
