import { ContentItemTypes, PickableEntity } from '@admin-ui/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { BaseFormElementComponent, ModalService, generateFormProvider } from '@gentics/ui-core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { ErrorHandler } from '@admin-ui/core';
import { EntityPickerModalComponent } from '../entity-picker-modal/entity-picker-modal.component';

@Component({
    selector: 'gtx-entity-picker',
    templateUrl: './entity-picker.component.html',
    styleUrls: ['./entity-picker.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(EntityPickerComponent)],
    standalone: false
})
export class EntityPickerComponent extends BaseFormElementComponent<PickableEntity | PickableEntity[]> {

    /**
     * Item-types which can be picked/selected.
     */
    @Input()
    public types: ContentItemTypes[] = [];

    /**
     * If this is `true`, and the `types` contains `'folder'`, then nodes and channels will
     * be selectable as well, but will be returned as the root folder.
     */
    @Input()
    public nodesAsFolder = false;

    /**
     * If multiple items can be picked.
     */
    @Input()
    public multiple = false;

    /**
     * If the picked items should be able to be cleared.
     */
    @Input()
    public clearable = true;

    /**
     * The resolved items to display in the input, to show the user what it currently selected.
     */
    public displayValue: PickableEntity[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        protected modals: ModalService,
        protected errorHandler: ErrorHandler,
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
            nodesAsFolder: this.nodesAsFolder,
        });

        try {
            const value = await dialog.open();

            if (value === false) {
                return;
            }

            this.triggerChange(value);
        } catch (err) {
            if (wasClosedByUser(err)) {
                return;
            }
            this.errorHandler.catch(err);
        }
    }

    clearValue(): void {
        this.triggerTouch();
        this.triggerChange(this.multiple ? [] : null);
    }
}
