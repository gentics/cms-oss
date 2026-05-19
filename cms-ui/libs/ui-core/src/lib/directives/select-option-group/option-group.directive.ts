import { booleanAttribute, ContentChildren, Directive, Input, QueryList } from '@angular/core';
import { randomId } from '@gentics/common';
import { SelectOptionDirective } from '../select-option/option.directive';

@Directive({
    selector: 'gtx-optgroup',
    standalone: false,
})
export class SelectOptionGroupDirective {

    public readonly UNIQUE_ID = `gtx-optgroup-${randomId()}`;

    @Input()
    public id: string = this.UNIQUE_ID;

    @Input()
    label: string;

    @Input({ transform: booleanAttribute })
    disabled: boolean;

    @ContentChildren(SelectOptionDirective)
    private _options: QueryList<SelectOptionDirective>;

    get options(): SelectOptionDirective[] {
        return this._options.toArray();
    }
}
