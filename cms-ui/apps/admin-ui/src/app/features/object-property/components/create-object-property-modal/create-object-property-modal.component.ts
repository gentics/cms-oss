import { ObjectPropertyHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ObjectProperty, ObjectPropertyCreateRequest } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { ObjectpropertyPropertiesMode } from '../object-property-properties/object-property-properties.component';

@Component({
    selector: 'gtx-create-object-property-modal',
    templateUrl: './create-object-property-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateObjectPropertyModalComponent implements IModalDialog, OnInit {

    public readonly ObjectpropertyPropertiesMode = ObjectpropertyPropertiesMode;

    /** form instance */
    form: UntypedFormControl;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: ObjectPropertyHandlerService,
    ) { }

    ngOnInit(): void {
        const payload: ObjectPropertyCreateRequest = {
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
        };
        // instantiate form
        this.form = new UntypedFormControl(payload);
    }

    closeFn = (entityCreated: ObjectProperty) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: ObjectProperty) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * If user clicks to create a new objectProperty
     */
    buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(objectPropertyCreated => {
                this.closeFn(objectPropertyCreated);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(): Promise<ObjectProperty> {
        return this.handler.createMapped(this.form.value).toPromise();
    }

}
