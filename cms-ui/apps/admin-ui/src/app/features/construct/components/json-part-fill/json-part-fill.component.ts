import { ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { AbstractControl, UntypedFormControl } from '@angular/forms';
import { TagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

const validateTagPartProperty: (type: TagPropertyType) => any = (type) => {
    return (control: AbstractControl): any => {
        let value: any = control.value;
        if (value == null) {
            return null;
        }
        if (typeof value !== 'string') {
            return { invalidType: typeof value };
        }
        value = value.trim();
        if (value.length === 0) {
            return null;
        }

        try {
            value = JSON.parse(value);
        } catch (err) {
            return { parseError: err.message };
        }

        if (value.type !== type) {
            return { invalidType: value.type };
        }

        return null;
    }
}

@Component({
    selector: 'gtx-json-part-fill',
    templateUrl: './json-part-fill.component.html',
    styleUrls: ['./json-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(JsonPartFillComponent)],
})
export class JsonPartFillComponent extends BaseFormElementComponent<TagPartProperty> implements OnInit, OnChanges {

    @Input()
    public type: TagPropertyType;

    public control: UntypedFormControl;

    ngOnInit(): void {
        this.control = new UntypedFormControl(JSON.stringify(this.value || {}), validateTagPartProperty(this.type));
        this.subscriptions.push(combineLatest([
            this.control.valueChanges,
            this.control.statusChanges,
        ]).pipe(
            map(([value, status]) => status === 'VALID' ? value : null),
            distinctUntilChanged(isEqual),
        ).subscribe(value => {
            if (value == null) {
                const parsed = JSON.parse(value);
                this.triggerChange(parsed);
            } else {
                this.triggerChange(value);
            }
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.type && this.control) {
            this.control.setValidators(validateTagPartProperty(this.type));
        }
    }

    protected onValueChange(): void {
        if (this.control) {
            this.control.setValue(JSON.stringify(this.value || {}));
        }
    }
}
