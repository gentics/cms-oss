import { ObjectPropertyCategoryHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ObjectPropertyCategory } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { ObjectPropertyCategoryPropertiesMode } from '../object-property-category-properties/object-property-category-properties.component';

@Component({
    selector: 'gtx-create-object-property-category-modal',
    templateUrl: './create-object-property-category-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateObjectPropertyCategoryModalComponent extends BaseModal<ObjectPropertyCategory> implements OnInit {

    public readonly ObjectPropertyCategoryPropertiesMode = ObjectPropertyCategoryPropertiesMode;

    public form: UntypedFormControl;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: ObjectPropertyCategoryHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({
            nameI18n: null,
        });
    }

    /**
     * If user clicks to create a new objectPropertyCategory
     */
    buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(created => {
                this.closeFn(created);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(): Promise<ObjectPropertyCategory> {
        return this.handler.createMapped(this.form.value).toPromise();
    }

}
