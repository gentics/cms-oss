import { SelectableType } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import {
    ListTagPartPropertyBase,
    SelectOption,
    SelectTagPartProperty,
    StringTagPartProperty,
    TagPartProperty,
    TagPropertyType,
} from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash';

const STRING_TYPES = [TagPropertyType.RICHTEXT, TagPropertyType.STRING];
const LIST_TYPES = [TagPropertyType.LIST, TagPropertyType.ORDEREDLIST, TagPropertyType.UNORDEREDLIST];
const SELECT_TYPES = [TagPropertyType.SELECT, TagPropertyType.MULTISELECT];

@Component({
    selector: 'gtx-construct-part-fill',
    templateUrl: './construct-part-fill.component.html',
    styleUrls: ['./construct-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ConstructPartFillComponent)],
})
export class ConstructPartFillComponent extends BaseFormElementComponent<TagPartProperty> implements OnChanges {

    readonly TagPropertyType = TagPropertyType;
    readonly SelectableTypes = SelectableType;

    @Input()
    public type: TagPropertyType;

    @Input()
    public multiple = false;

    @Input()
    public selectOptions: SelectOption[] = [];

    private oldType: TagPropertyType;

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push('multiple');
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.type && this.type !== this.oldType) {
            if (this.oldType != null) {
                this.onTypeChange();
            } else {
                this.oldType = this.type;
            }
        }
    }

    onTypeChange(): void {
        const newValue: TagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: this.type,
        } as TagPartProperty;

        // Certain types save the value into the same property and are still valid to be displayed/used there.
        // For these special cases, we use this to keep the value across these types.
        // Other ones are getting cleared, to have accurate part-properties in the parent form.
        // If we don't do this, on type change, the value wouldn't change and cause errors on updates if the user
        // doesn't update the default properties (as they seem to be empty to the user anyways).
        if (STRING_TYPES.includes(this.oldType)) {
            if (STRING_TYPES.includes(this.type)) {
                (newValue as StringTagPartProperty).stringValue = ((this.value || {}) as StringTagPartProperty).stringValue;
            }
        } else if (LIST_TYPES.includes(this.oldType)) {
            if (LIST_TYPES.includes(this.type)) {
                (newValue as ListTagPartPropertyBase).stringValues = ((this.value || {}) as ListTagPartPropertyBase).stringValues;
            }
        } else if (SELECT_TYPES.includes(this.oldType)) {
            if (SELECT_TYPES.includes(this.type)) {
                (newValue as SelectTagPartProperty).selectedOptions = ((this.value || {}) as SelectTagPartProperty).selectedOptions;
            }
        }

        this.oldType = this.type;
        this.triggerChange(newValue);
    }

    protected onValueChange(): void { }

}
