import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    QueryList,
    Renderer2,
    ViewChild,
    ViewChildren
} from '@angular/core';
import { UntypedFormControl, SelectControlValueAccessor } from '@angular/forms';
import { generateFormProvider } from '@gentics/ui-core';
import { isEqual as _isEqual } from'lodash-es'
import { takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';

export interface GtxInputSelectOption {
    id: string;
    value: any;
    label: string;
}

/**
 * Custom input select implementation to support `formControlName` directive.
 */
@Component({
    selector: 'gtx-input-select',
    templateUrl: './input-select.component.html',
    styleUrls: ['./input-select.component.scss'],
    providers: [generateFormProvider(InputSelectComponent)],
})
export class InputSelectComponent extends SelectControlValueAccessor implements AfterViewInit, OnDestroy, OnInit {

    /** Unique component ID */
    compId = Math.floor(Math.random() * 100);

    control: UntypedFormControl;

    @Input() options: GtxInputSelectOption[] = [];

    /** If TRUE made first value of options preselected */
    @Input() defaultValue: string;

    /** If TRUE, input mimics disabled behaviour */
    @Input() disabled: boolean;

    /** If TRUE, input appears not as input but as normal label */
    @Input() noInput: boolean;

    /** If amount of menu items is larger than this property's value, an input text field for filtering is displayed */
    @Input() displayFilterThreshold = 6;

    @Output() ngModelChange = new EventEmitter<string>();

    @ViewChild('dropDownMenuItemsFilterElement') dropDownMenuItemsFilterElement: ElementRef;

    @ViewChildren('dropDownMenuItems') dropDownMenuItems: QueryList<ElementRef>;

    @Input() iconOnly = false;

    dropDownMenuIsOpen = false;

    dropDownMenuItemsFilterTerm = '';

    private stopper = new ObservableStopper();

    constructor(
        _renderer: Renderer2,
        _elementRef: ElementRef,
    ) {
        super(_renderer, _elementRef);
    }

    ngOnInit(): void {

        this.control = new UntypedFormControl('');

        this.control.valueChanges.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((v: string) => {
            this.onChange(v);
        });
    }

    ngAfterViewInit(): void {
        if (typeof this.defaultValue === 'string') {
            this.writeValue(this.defaultValue);
        }
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    @Input() set compareWith(fn: (o1: any, o2: any) => boolean) {
        _isEqual.apply(this, fn.arguments);
    }

    @Input() onChange: (newValue: string) => void = (newValue: any): void => {
        if (typeof newValue === 'string') {
            this.ngModelChange.emit(newValue);
        }
    }

    /** Implements `SelectControlValueAccessor`-interface method */
    setDisabledState(isDisabled: boolean): void {
        this.dropDownMenuIsOpen = false;
        this.disabled = isDisabled;
    }

    writeValue(value: string): void {
        const option = typeof value === 'string' && this.options.find(option => option.value === value);
        this.value = option && typeof option.value === 'string' ? option.value : null;
        this.control.setValue(this.value);
        this.control.updateValueAndValidity();
    }

    dropDownMenuOptionIsVisible(labelTranslated: string): boolean {
        if (this.dropDownMenuItemsFilterIsDisplayed()) {
            const pattern = new RegExp(this.dropDownMenuItemsFilterTerm, 'gi');
            return pattern.test(labelTranslated);
        } else {
            return true;
        }
    }

    dropDownMenuTrigger(event?: Event): void {
        if (event) {
            event.preventDefault();
        }
        if (this.disabled || this.noInput) {
            return;
        }
        this.dropDownMenuIsOpen = !this.dropDownMenuIsOpen;
        if (this.dropDownMenuIsOpen) {
            setTimeout(() => this.dropDownMenuItemsFilterElement.nativeElement.focus(), 100);
        }
    }

    dropDownMenuClose(): void {
        this.dropDownMenuIsOpen = false;
        this.dropDownMenuItemsFilterTerm = '';
    }

    onDropDownMenuItemTriggered(value: string, event?: Event): void {
        if (this.disabled || this.noInput || !this.dropDownMenuIsOpen) {
            return;
        }
        this.dropDownMenuIsOpen = false;
        this.writeValue(value);
        this.dropDownMenuItemsFilterTerm = '';
    }

    labelGetByValue(value: string): string {
        const option = this.options.find(o => o.value === value);
        return option ? option.label : null;
    }

    dropDownMenuItemFocus(index: number): void {
        const itemsAmount = this.dropDownMenuItems.length;
        if (index > itemsAmount - 1) {
            index = 0;
        }
        if (index < 0) {
            index = itemsAmount - 1;
        }
        const elem = this.dropDownMenuItems.toArray()[index];
        if (elem && elem.nativeElement) {
            elem.nativeElement.focus();
        }
    }

    dropDownMenuItemsFilterIsDisplayed(): boolean {
        if (!Array.isArray(this.options)) {
            return;
        }
        const optionsAmount = this.options.length;
        return optionsAmount > this.displayFilterThreshold;
    }

}
