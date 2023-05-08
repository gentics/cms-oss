import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ListTagPartProperty, OrderedUnorderedListTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash';

@Component({
    selector: 'gtx-list-part-fill',
    templateUrl: './list-part-fill.component.html',
    styleUrls: ['./list-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ListPartFillComponent)],
})
export class ListPartFillComponent extends BaseFormElementComponent<ListTagPartProperty | OrderedUnorderedListTagPartProperty> {

    @Input()
    public type: TagPropertyType.LIST | TagPropertyType.UNORDEREDLIST | TagPropertyType.ORDEREDLIST;

    protected onValueChange(): void { }

    listUpdated(values: string[]): void {
        const newValue: OrderedUnorderedListTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: this.type,
            stringValues: values,
        } as OrderedUnorderedListTagPartProperty;

        // TODO: Add the select for ordered/unordered?
        if (this.type === TagPropertyType.LIST) {
            (newValue as any).booleanValue = false;
        }

        this.triggerChange(newValue);
    }

}
