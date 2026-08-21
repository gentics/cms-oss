import { ObjectPropertyHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditableObjectProperty, ObjectProperty } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { ObjectpropertyPropertiesMode } from '../object-property-properties/object-property-properties.component';

@Component({
    selector: 'gtx-create-object-property-modal',
    templateUrl: './create-object-property-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateObjectPropertyModalComponent extends BaseModal<ObjectProperty> implements OnInit {

    public readonly ObjectpropertyPropertiesMode = ObjectpropertyPropertiesMode;

    /** form instance */
    form: FormControl<EditableObjectProperty>;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: ObjectPropertyHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new FormControl({
            nameI18n: null,
            descriptionI18n: null,
            keyword: '',
            type: null,
            constructId: null,
            categoryId: null,
            required: false,
            inheritable: false,
            syncContentset: false,
            syncChannelset: false,
            syncVariants: false,
            restricted: false,
        });
    }

    /**
     * If user clicks to create a new objectProperty
     */
    buttonCreateEntityClicked(): void {
        const data = this.form.value;
        this.form.disable();
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity(data)
            .then(objectPropertyCreated => {
                this.closeFn(objectPropertyCreated);
            })
            .catch(() => {
                this.form.enable();
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(data: EditableObjectProperty): Promise<ObjectProperty> {
        return this.handler.createMapped(data).toPromise();
    }
}
