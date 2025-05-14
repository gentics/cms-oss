import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { KeyCode } from '../../common';
import { cancelEvent, coerceToBoolean, generateFormProvider } from '../../utils';

/**
 * The SearchBar component should be the primary search input for the app. It should be
 * located near the top of the screen, below the [TopBar](#/top-bar).
 *
 * ```html
 * <gtx-search-bar [query]="searchQuery"
 *                 (change)="onChange($event)"
 *                 (search)="search($event)">
 * </gtx-search-bar>
 * ```
 *
 * ## Use With NgModel
 * The search query can be bound with `NgModel`, which can be useful for implementing a reset function:
 *
 * ```html
 * <gtx-search-bar [(ngModel)]="searchQuery"
 *                 (clear)="searchQuery = ''">
 * </gtx-search-bar>
 * ```
 *
 * ## Content Projection
 * Content inside the `<gtx-search-bar>` tags will be projected inside the component, to the left of the
 * search bar. This can be used, for example, to display current filters being applied to the search.
 *
 * ```html
 * <gtx-search-bar>
 *      <div class="chip">Tag 1<i class="material-icons">close</i></div>
 * </gtx-search-bar>
 * ```
 *
 * ## Custom Icons
 * Icons in the `<gtx-search-bar>` can be replaced with custom ones.
 *
 * ```html
 * <gtx-search-bar submitIcon="filter_list"
 *                 clearIcon="undo">
 * </gtx-search-bar>
 * ```
 */
@Component({
    selector: 'gtx-search-bar',
    templateUrl: './search-bar.component.html',
    styleUrls: ['./search-bar.component.scss'],
    providers: [generateFormProvider(SearchBarComponent)],
    standalone: false
})
export class SearchBarComponent implements ControlValueAccessor, OnChanges {

    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly cancelEvent = cancelEvent;

    /**
     * Sets the input field to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * Value that pre-fills the search input with a string value.
     */
    @Input()
    public query = '';

    /**
     * Sets the icon displayed for the submit button
     */
    @Input()
    public submitIcon = 'search';

    /**
     * Sets the icon displayed for the clear button
     */
    @Input()
    public clearIcon = 'close';

    /**
     * Placeholder text which is shown when no text is entered.
     */
    @Input()
    public placeholder = 'Search';

    /**
     * Setting this attribute will prevent the "clear" button from being displayed
     * when the query is non-empty.
     */
    @Input()
    public hideClearButton: boolean;

    /**
     * Fired when either the search button is clicked, or
     * the "enter" key is pressed while the input has focus.
     */
    @Output()
    public search = new EventEmitter<string>();

    /**
     * Fired whenever the value of the input changes.
     */
    @Output()
    public change = new EventEmitter<string>();

    /**
     * Fired when the clear button is clicked.
     */
    @Output()
    public clear = new EventEmitter<boolean>();

    private cvaChange: (value: string) => void = () => {
        // noop
    };
    private cvaTouch: () => void = () => {
        // noop
    };

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.hideClearButton) {
            this.hideClearButton = coerceToBoolean(this.hideClearButton);
        }
    }

    doSearch(): void {
        this.search.emit(this.query);
    }

    /**
     * Handler for pressing "enter" key.
     */
    onKeyDown(event: KeyboardEvent): void {
        if (event.keyCode === KeyCode.Enter) {
            this.doSearch();
        }
    }

    onInputChange(event: string): void {
        this.query = event;
        if (typeof event === 'string') {
            this.cvaChange(event);
            this.change.emit(event);
        }
    }

    onInputBlur(event: string): void {
        if (typeof event === 'string') {
            this.cvaTouch();
        }
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    writeValue(value: any): void {
        this.query = value;
        this.changeDetector.markForCheck();
    }

    registerOnChange(fn: (value: string) => void): void {
        this.cvaChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.cvaTouch = fn;
    }
}
