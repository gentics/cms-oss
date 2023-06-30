import { ContentItemTypes, PickableEntity } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { BaseFormElementComponent, ModalService, generateFormProvider } from '@gentics/ui-core';
import { EntityPickerModalComponent } from '../entity-picker-modal/entity-picker-modal.component';

@Component({
    selector: 'gtx-entity-picker',
    templateUrl: './entity-picker.component.html',
    styleUrls: ['./entity-picker.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(EntityPickerComponent)],
})
export class EntityPickerComponent extends BaseFormElementComponent<PickableEntity | PickableEntity[]> {

    @Input()
    public types: ContentItemTypes[] = [];

    @Input()
    public multiple = false;

    @Input()
    public clearable = true;

    public displayValue: PickableEntity[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        protected modals: ModalService,
    ) {
        super(changeDetector);
        this.booleanInputs.push('multiple', 'clearable');
    }

    onValueChange(): void {
        if (this.value == null) {
            this.displayValue = [];
        } else if (Array.isArray(this.value)) {
            this.displayValue = this.value;
        } else {
            this.displayValue = [this.value];
        }
    }

    async openPickerModal(): Promise<void> {
        this.triggerTouch();

        const dialog = await this.modals.fromComponent(EntityPickerModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        }, {
            types: this.types,
            multiple: this.multiple,
            selected: this.value,
        });

        const value = await dialog.open();

        if (value === false) {
            return;
        }

        this.triggerChange(value);
    }

    clearValue(): void {
        this.triggerTouch();
        this.triggerChange(this.multiple ? [] : null);
    }
}
