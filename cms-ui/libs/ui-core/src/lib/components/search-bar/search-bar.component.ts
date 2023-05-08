import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { KeyCode } from '../../common';
import { generateFormProvider } from '../../utils';

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
})
export class SearchBarComponent implements ControlValueAccessor {

    /**
     * Sets the input field to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    autofocus = false;

    /**
     * Value that pre-fills the search input with a string value.
     */
    @Input()
    query = '';

    /**
     * Sets the icon displayed for the submit button
     */
    @Input()
    submitIcon = 'search';

    /**
     * Sets the icon displayed for the clear button
     */
    @Input()
    clearIcon = 'close';

    /**
     * Placeholder text which is shown when no text is entered.
     */
    @Input()
    placeholder = 'Search';

    /**
     * Setting this attribute will prevent the "clear" button from being displayed
     * when the query is non-empty.
     */
    @Input()
    get hideClearButton(): boolean {
        return this._hideClearButton === true;
    }
    set hideClearButton(val: boolean) {
        this._hideClearButton = val != null && val !== false;
    }

    /**
     * Fired when either the search button is clicked, or
     * the "enter" key is pressed while the input has focus.
     */
    @Output()
    search = new EventEmitter<string>();

    /**
     * Fired whenever the value of the input changes.
     */
    @Output()
    change = new EventEmitter<string>();

    /**
     * Fired when the clear button is clicked.
     */
    @Output()
    clear = new EventEmitter<boolean>();

    private _hideClearButton = false;

    // ValueAccessor members
    onChange: any = (_: any) => { };
    onTouched: any = () => { };

    constructor(private changeDetector: ChangeDetectorRef) { }

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
            this.onChange(event);
            this.change.emit(event);
        }
    }

    onInputBlur(event: string): void {
        if (typeof event === 'string') {
            this.onTouched(event);
        }
    }

    writeValue(value: any): void {
        this.query = value;
        this.changeDetector.markForCheck();
    }

    registerOnChange(fn: Function): void { this.onChange = fn; }
    registerOnTouched(fn: Function): void { this.onTouched = fn; }
}
