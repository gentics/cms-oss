import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Schema } from '@gentics/mesh-models';
import { BaseFormElementComponent, ModalService, generateFormProvider } from '@gentics/ui-core';
import { SelectSchemaModal } from '../select-schema-modal/select-schema-modal.component';

function createDisplayValue(schemas: Schema[]): string {
    return schemas.length === 0 ? null : schemas
        .map(schema => schema.name)
        .sort((a, b) => a.localeCompare(b))
        .join(', ');
}

@Component({
    selector: 'gtx-mesh-schema-picker',
    templateUrl: './schema-picker.component.html',
    styleUrls: ['./schema-picker.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SchemaPickerComponent)],
    standalone: false
})
export class SchemaPickerComponent extends BaseFormElementComponent<Schema | Schema[]> {

    @Input()
    public multiple = false;

    @Input()
    public clearable: boolean | null = null;

    @Input()
    public placeholder: string;

    public valueArray: Schema[] = [];
    public displayValue: string;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected modal: ModalService,
    ) {
        super(changeDetector);
        this.booleanInputs.push('multiple');
    }

    protected onValueChange(): void {
        if (this.value == null) {
            this.valueArray = [];
            this.displayValue = null;
            return;
        }

        if (Array.isArray(this.value)) {
            this.valueArray = this.value.filter(v => v != null && typeof v === 'object');
        } else if (typeof this.value === 'object') {
            this.valueArray = [this.value];
        } else {
            this.valueArray = [];
            this.displayValue = null;
            return;
        }

        if (!this.multiple && this.valueArray.length > 1) {
            this.valueArray = [this.valueArray[0]];
        }

        this.displayValue = createDisplayValue(this.valueArray);
    }

    async pickNewValues(): Promise<void> {
        this.triggerTouch();

        const dialog = await this.modal.fromComponent(SelectSchemaModal, {}, {
            title: 'mesh.select_schema',
            selected: this.valueArray.map(schema => schema.uuid),
            multiple: this.multiple,
        });

        let schemas = (await dialog.open()) || [];
        if (!Array.isArray(schemas)) {
            schemas = [schemas];
        }

        if (!this.pure) {
            this.valueArray = schemas;
            this.displayValue = createDisplayValue(this.valueArray);
        }
        this.triggerChange(this.multiple ? schemas : schemas[0]);
    }

    clearValue(): void {
        this.triggerTouch();

        if (!this.pure) {
            this.valueArray = [];
            this.displayValue = null;
        }

        this.triggerChange([]);
    }
}
