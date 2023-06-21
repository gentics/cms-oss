import {
    AfterContentInit,
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    EventEmitter,
    Input,
    Output,
    QueryList,
    ViewChild,
} from '@angular/core';
import { isEqual } from 'lodash-es';
import { IncludeToDocs, KeyCode } from '../../common';
import { SelectOptionGroupDirective } from '../../directives/select-option-group/option-group.directive';
import { SelectOptionDirective } from '../../directives/select-option/option.directive';
import { generateFormProvider, getValueByPath } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';
import { DropdownContentComponent } from '../dropdown-content/dropdown-content.component';
import { DropdownListComponent } from '../dropdown-list/dropdown-list.component';

export interface NormalizedOptionGroup {
    options: SelectOptionDirective[];
    label: string;
    disabled: boolean;
    isDefaultGroup: boolean;
}

export type SelectedSelectOption = [number, number];

type SingleOrArray<T> = T | T[];

/**
 * A Select form control which works with any kind of value - as opposed to the native HTML `<select>` which only works
 * with strings. The Select control depends on the [`<gtx-overlay-host>`](#/overlay-host) being present in the app.
 *
 * ```html
 * <gtx-select label="Choose an option" [(ngModel)]="selectVal">
 *     <gtx-option
 *         *ngFor="let item of options"
 *         [value]="item"
 *         [disabled]="item.disabled"
 *     >{{ item.label }}</gtx-option>
 * </gtx-select>
 * ```
 *
 */
@Component({
    selector: 'gtx-select',
    templateUrl: './select.component.html',
    styleUrls: ['./select.component.scss'],
    providers: [generateFormProvider(SelectComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectComponent
    extends BaseFormElementComponent<SingleOrArray<string | number>>
    implements AfterViewInit, AfterContentInit {

    /**
     * Path to the id of the object (if objects are used as options).
     */
    @Input()
    public idPath: string | symbol | (string | symbol)[] = null;

    /**
     * Sets the select box to be auto-focused. Handled by `AutofocusDirective`.
     */
    @Input()
    public autofocus = false;

    /**
     * If true, the clear button is displayed, which allows the user to clear the selection.
     */
    @Input()
    public clearable = false;

    /**
     * If true, the select all button is displayed, which allows the user to select all options at once.
     */
    @Input()
    public selectAll = false;

    /**
     * When set to true, allows multiple options to be selected. In this case, the input value should be
     * an array of strings; events will emit an array of strings.
     */
    @Input()
    public multiple = false;

    /**
     * Placeholder which is shown if nothing is selected.
     */
    @Input()
    public placeholder = '';

    /**
     * Blur event.
     */
    @Output()
    public blur = new EventEmitter<any>();

    /**
     * Focus event.
     */
    @Output()
    public focus = new EventEmitter<any>();

    @ViewChild(DropdownListComponent, { static: true })
    private dropdownList: DropdownListComponent;

    @ViewChild(DropdownContentComponent, { static: true })
    private dropdownContent: DropdownContentComponent;

    @ContentChildren(SelectOptionDirective, { descendants: false })
    private selectOptions: QueryList<SelectOptionDirective>;

    @ContentChildren(SelectOptionGroupDirective, { descendants: false })
    private selectOptionGroups: QueryList<SelectOptionGroupDirective>;

    private valueArray: (number | string)[] = [];

    // An array of abstracted containers for options, which allows us to treat options and groups in a
    // consistent way.
    optionGroups: NormalizedOptionGroup[] = [];

    selectedOptions: SelectOptionDirective[] = [];
    viewValue = '';

    // Keeps track of the selected option. Two dimensional because options may be nested inside groups. The first
    // value is the index of the group (-1 is the default "no group" group), and the second number is the index
    // of the option within that group.
    selectedIndex: SelectedSelectOption = [0, -1];

    private preventDeselect = false;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('clearable', 'selectAll', 'multiple');
    }

    ngAfterViewInit(): void {
        // Update the value if there are any changes to the options
        this.subscriptions.push(
            this.selectOptions.changes.subscribe(() => {
                this.writeValue(this.value);
                this.optionGroups = this.buildOptionGroups();
                this.selectedOptions = this.getInitiallySelectedOptions();
                this.updateViewValue();
            }),
        );
    }

    ngAfterContentInit(): void {
        this.optionGroups = this.buildOptionGroups();
        this.selectedOptions = this.getInitiallySelectedOptions();
        this.updateViewValue();
    }

    /**
     * Event handler for when one of the Materialize-generated LI elements is clicked.
     */
    @IncludeToDocs()
    selectItem(groupIndex: number, optionIndex: number): void {
        const option = this.optionGroups[groupIndex] && this.optionGroups[groupIndex].options[optionIndex];
        if (!this.optionGroups[groupIndex].disabled && option && !option.disabled) {
            this.toggleSelectedOption(option);
            const selectedValues = this.selectedOptions.map(o => o.value);
            this.triggerChange(this.multiple ? selectedValues : selectedValues[0]);
            this.scrollToSelectedOption();
        }
    }

    protected onValueChange(): void {
        if (!Array.isArray(this.value)) {
            this.valueArray = this.value ? [this.value] : [];
        } else {
            this.valueArray = this.value;
        }

        const selectOptions: SelectOptionDirective[] = [];
        if (this.selectOptions) {
            selectOptions.push(...this.selectOptions.toArray());
        }
        if (this.selectOptionGroups) {
            this.selectOptionGroups.toArray().forEach(group => {
                selectOptions.push(...group.options);
            });
        }

        if (selectOptions) {
            let tmp = selectOptions.filter(option => {
                for (const selectedValue of this.valueArray) {
                    return this.isSame(selectedValue, option.value);
                }
                return false;
            });
            if (!this.multiple && tmp.length > 1) {
                tmp = tmp.slice(0, 1);
            }
            this.selectedOptions = tmp;
        }

        this.updateViewValue();
    }

    private isSame(value1: any, value2: any): boolean {
        if ((value1 == null && value2 != null) || (value1 != null && value2 == null)) {
            return false;
        }
        if (typeof value1 === 'object' && typeof value2 === 'object') {
            if (this.idPath != null) {
                return getValueByPath(value1, this.idPath) === getValueByPath(value2, this.idPath);
            }
            return isEqual(value1, value2);
        }

        return value1 === value2;
    }

    override triggerChange(value: SingleOrArray<string | number>): void {
        super.triggerChange(value);
        this.updateViewValue();
    }

    inputBlur(e: Event): void {
        e.stopPropagation();
        this.triggerTouch();
        this.blur.emit(this.value);
    }

    /**
     * Select the initial value when the dropdown is opened.
     */
    dropdownOpened(): void {
        if (this.selectedOptions.length < 1) {
            return;
        }

        this.preventDeselect = true;
        const selected = this.selectedOptions[0];
        this.selectedIndex = this.getIndexFromSelectOption(selected);
        setTimeout(() => {
            this.scrollToSelectedOption();
            this.preventDeselect = false;
        }, 100);
    }

    /**
     * Handle keydown events to enable keyboard navigation and selection of options.
     */
    handleKeydown(event: KeyboardEvent): void {
        if (event.ctrlKey || event.altKey || event.metaKey) {
            return;
        }

        const keyCode = event.keyCode;

        switch (keyCode) {
            case KeyCode.UpArrow:
                this.updateSelectedIndex(this.getPreviousIndex(this.selectedIndex));
                break;

            case KeyCode.DownArrow:
                this.updateSelectedIndex(this.getNextIndex(this.selectedIndex));
                break;

            case KeyCode.PageUp:
            case KeyCode.Home:
                this.updateSelectedIndex(this.getFirstIndex());
                break;

            case KeyCode.PageDown:
            case KeyCode.End:
                this.updateSelectedIndex(this.getLastIndex());
                break;

            case KeyCode.Enter:
            case KeyCode.Space:
                if (!this.dropdownList.isOpen) {
                    this.dropdownList.openDropdown();
                } else {
                    this.selectItem(this.selectedIndex[0], this.selectedIndex[1]);
                    if (!this.multiple) {
                        this.dropdownList.closeDropdown();
                    }
                }
                break;

            default: {
                // Other keys are treated as if the user is trying to jump to an option by character
                const indexOfMatch = this.searchByKey(event.key);
                if (indexOfMatch) {
                    this.updateSelectedIndex(indexOfMatch);
                }
            }

        }
    }

    isSelected(option: SelectOptionDirective): boolean {
        return this.selectedOptions.findIndex(selected => {
            return this.isSame(option.value, selected.value);
        }) > -1;
    }

    deselect(): void {
        if (!this.preventDeselect) {
            this.selectedIndex = [0, -1];
        }
    }

    /** Clears the selected value and emits `null` with the `change` event. */
    clearSelection(): void {
        if (this.disabled) {
            return;
        }

        this.triggerChange(this.multiple ? [] : null);
    }

    selectAllOptions(): void {
        this.triggerChange(this.selectOptions.map(option => option.value));
    }

    /**
     * Given a SelectOption, returns the position in the 2D selectedIndex array.
     */
    private getIndexFromSelectOption(selected: SelectOptionDirective): SelectedSelectOption {
        if (!selected) {
            return [0, 0];
        }

        let selectedGroup = 0;
        let selectedOption = 0;

        for (let i = 0; i < this.optionGroups.length; i++) {
            const group = this.optionGroups[i];
            selectedGroup = i;
            for (let j = 0; j < group.options.length; j++) {
                const option = group.options[j];
                selectedOption = j;
                if (option === selected) {
                    return [selectedGroup, selectedOption];
                }
            }
        }
    }

    /**
     * Once the contents have been compiled, we can build up the optionGroups array, grouping options into
     * a "default" group, i.e. the group of options which are not children of a <gtx-optgroup>, and then any
     * other groups as specified by optgroups.
     */
    private buildOptionGroups(): NormalizedOptionGroup[] {
        const groups = this.selectOptionGroups.map(g => {
            return {
                get options(): SelectOptionDirective[] { return g.options; },
                get label(): string { return g.label; },
                get disabled(): boolean { return g.disabled; },
                isDefaultGroup: false,
            };
        });

        if (this.selectOptions.length) {
            groups.unshift({
                options: this.selectOptions.toArray(),
                label: '',
                isDefaultGroup: true,
                disabled: false,
            });
        }
        return groups;
    }

    /**
     * Select any options which match the value passed in via the `value` attribute.
     */
    private getInitiallySelectedOptions(): SelectOptionDirective[] {
        let selectedOptions: SelectOptionDirective[] = [];
        const flatOptionsList = this.optionGroups.reduce(
            (options, group) => options.concat(group.options), []);

        if (this.value !== undefined) {
            if (this.multiple) {
                if (this.value instanceof Array) {
                    selectedOptions = flatOptionsList.filter(o => this.valueArray.includes(o.value));
                }
            } else {
                selectedOptions = flatOptionsList.filter(o => this.value === o.value) || [];
                return flatOptionsList.filter(o => this.value === o.value) || [];
            }
        }
        return selectedOptions;
    }

    /**
     * Toggle the selection of the given SelectOption, taking into account whether this is a multiple
     * select.
     */
    private toggleSelectedOption(option: SelectOptionDirective): void {
        if (!this.multiple) {
            this.selectedOptions = [];
        }
        const index = this.selectedOptions.findIndex(selected => {
            return this.isSame(selected.value, option.value);
        });
        if (-1 < index) {
            // de-select the existing option
            this.selectedOptions.splice(index, 1);
        } else {
            this.selectedOptions.push(option);
        }
    }

    private updateViewValue(): void {
        this.viewValue = this.selectedOptions.map(o => o.viewValue).join(', ');
        this.changeDetector.markForCheck();
    }

    /**
     * When a list of options is too long, there will be a scroll bar. This method ensures that the currently-selected
     * options is scrolled into view in the options list.
     */
    private scrollToSelectedOption(): void {
        setTimeout(() => {
            const container: HTMLElement = this.dropdownContent.elementRef.nativeElement;
            const selectedItem: HTMLLIElement = container.querySelector('li.selected');
            if (selectedItem) {
                const belowContainer = container.offsetHeight + container.scrollTop < selectedItem.offsetTop + selectedItem.offsetHeight;
                const aboveContainer = selectedItem.offsetTop < container.scrollTop;

                if (belowContainer) {
                    container.scrollTop = selectedItem.offsetTop + selectedItem.offsetHeight - container.offsetHeight;
                }
                if (aboveContainer) {
                    container.scrollTop = selectedItem.offsetTop;
                }
            }
        });
    }

    /**
     * Searches through the available options and locates the next option with a viewValue whose first character
     * matches the character passed in. Useful for jumping to options quickly by typing the first letter of the
     * option view value.
     */
    private searchByKey(key: string): SelectedSelectOption {
        const keyUpperCase = key.toLocaleUpperCase();
        const totalOptionCount = this.optionGroups.reduce((total, group) => total + group.options.length, 0);
        let currentIndex = this.selectedIndex.slice() as SelectedSelectOption;

        for (let counter = 0; counter < totalOptionCount; counter++) {
            currentIndex = this.getNextIndex(currentIndex);
            const option = this.optionGroups[currentIndex[0]].options[currentIndex[1]];
            const firstLetterUppercase = option.viewValue.charAt(0).toLocaleUpperCase();

            if (firstLetterUppercase === keyUpperCase) {
                return currentIndex;
            }
        }
    }

    private getFirstIndex(): SelectedSelectOption {
        return [0, 0];
    }

    private getLastIndex(): SelectedSelectOption {
        const lastGroupIndex = this.optionGroups.length - 1;
        return [lastGroupIndex, this.optionGroups[lastGroupIndex].options.length - 1];
    }

    private getNextIndex(currentIndex: SelectedSelectOption): SelectedSelectOption {
        let nextIndex = currentIndex.slice() as SelectedSelectOption;
        const isLastGroup = currentIndex[0] === this.optionGroups.length - 1;
        const isLastOptionInGroup = currentIndex[1] === this.optionGroups[currentIndex[0]].options.length - 1;
        if (isLastOptionInGroup) {
            if (isLastGroup) {
                nextIndex = this.getFirstIndex();
            } else {
                nextIndex[0]++;
                nextIndex[1] = 0;
            }
        } else {
            nextIndex[1]++;
        }
        return nextIndex;
    }

    private getPreviousIndex(currentIndex: SelectedSelectOption): SelectedSelectOption {
        let nextIndex = currentIndex.slice() as SelectedSelectOption;
        if (currentIndex[0] <= 0) {
            if (0 < currentIndex[1]) {
                nextIndex[1]--;
            } else {
                nextIndex = this.getLastIndex();
            }
        } else {
            if (0 < currentIndex[1]) {
                nextIndex[1]--;
            } else {
                nextIndex[0]--;
                nextIndex[1] = this.optionGroups[currentIndex[0]].options.length - 1;
            }
        }
        return nextIndex;
    }

    /**
     * Sets the `selectedOptions` array to contain the single option at the selectedIndex.
     */
    private updateSelectedIndex(index: SelectedSelectOption): void {
        this.selectedIndex = index;
        const options = this.optionGroups[index[0]].options;

        if (options && 0 <= index[1] && index[1] < options.length) {
            if (!this.multiple) {
                this.selectItem(index[0], index[1]);
            } else {
                this.scrollToSelectedOption();
            }
        }
    }
}
