import { ObjectPropertyCategoryOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { ObjectPropertyCategoryBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { ObjectPropertyCategoryPropertiesMode } from '../object-property-category-properties/object-property-category-properties.component';

@Component({
    selector: 'gtx-create-object-property-category-modal',
    templateUrl: './create-object-property-category-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateObjectPropertyCategoryModalComponent extends BaseModal<ObjectPropertyCategoryBO> implements OnInit {

    public readonly ObjectPropertyCategoryPropertiesMode = ObjectPropertyCategoryPropertiesMode;

    public form: UntypedFormControl;

    constructor(
        private entityOperations: ObjectPropertyCategoryOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({
            nameI18n: null,
        }, createNestedControlValidator());
    }

    /**
     * If user clicks to create a new objectPropertyCategory
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(created => this.closeFn(created));
    }

    private createEntity(): Promise<ObjectPropertyCategoryBO> {
        return this.entityOperations.create(this.form.value).toPromise();
    }

}
