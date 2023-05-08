import { ContentChildren, Directive, Input, QueryList } from '@angular/core';
import { coerceToBoolean } from '../../utils';
import { SelectOptionDirective } from '../select-option/option.directive';

@Directive({
    selector: 'gtx-optgroup',
})
export class SelectOptionGroupDirective {

    @Input()
    label: string;

    @Input()
    set disabled(value: boolean) {
        this._disabled = coerceToBoolean(value);
    }
    get disabled(): boolean {
        return this._disabled;
    }

    private _disabled: any;

    @ContentChildren(SelectOptionDirective)
    private _options: QueryList<SelectOptionDirective>;

    get options(): SelectOptionDirective[] {
        return this._options.toArray();
    }
}
