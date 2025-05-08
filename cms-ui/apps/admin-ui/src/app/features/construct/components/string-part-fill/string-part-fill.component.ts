import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { StringTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from'lodash-es'

@Component({
    selector: 'gtx-string-part-fill',
    templateUrl: './string-part-fill.component.html',
    styleUrls: ['./string-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(StringPartFillComponent)],
    standalone: false
})
export class StringPartFillComponent extends BaseFormElementComponent<StringTagPartProperty> {

    @Input()
    public type: TagPropertyType.STRING | TagPropertyType.RICHTEXT;

    protected onValueChange(): void { }

    textUpdated(value: string): void {
        const newValue: StringTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: this.type,
            stringValue: value,
        };

        this.triggerChange(newValue);
    }
}
