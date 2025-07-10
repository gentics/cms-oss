import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'search-suggestion',
    templateUrl: './search-suggestion.component.html',
    styleUrls: ['./search-suggestion.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SearchSuggestionComponent {

    /** Icon left of the suggestion text */
    @Input() icon: string;

    /** A small optional item on the bottom right of the regular item */
    @Input() iconSecondary: string;

    /** Grayed-out text to the right of the suggestion text */
    @Input() context: string;

    /** If set, the passed text is used to offset the content from the left (e.g. a filter) */
    @Input() sizePrefix: string;

    /** User clicked the suggestion or selected it with the keyboard and pressed [Enter] */
    @Output() use = new EventEmitter<void>();

    isSelected = false;

    constructor(private changeDetector: ChangeDetectorRef) { }

    /** Will be called by SearchSuggestionBar */
    setSelected(selected: boolean): void {
        if (selected !== this.isSelected) {
            this.isSelected = selected;
            this.changeDetector.detectChanges();
        }
    }

}
