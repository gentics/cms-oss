import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BooleanTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash';

@Component({
    selector: 'gtx-boolean-part-fill',
    templateUrl: './boolean-part-fill.component.html',
    styleUrls: ['./boolean-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(BooleanPartFillComponent)],
})
export class BooleanPartFillComponent extends BaseFormElementComponent<BooleanTagPartProperty> {

    protected onValueChange(): void {}

    toggleValue(event: MouseEvent): void {
        event.preventDefault();
        event.stopImmediatePropagation();
        event.stopPropagation();

        const newValue: BooleanTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: TagPropertyType.BOOLEAN,
            booleanValue: !this.value.booleanValue,
        };

        this.triggerChange(newValue);
    }
}
