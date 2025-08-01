import { ContentRepositoryFragmentOperations } from '@admin-ui/core';
import { EntityExistsValidator } from '@admin-ui/shared/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ContentRepositoryFragmentBO, ContentRepositoryFragmentCreateRequest, Normalized } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-cr-fragment-modal',
    templateUrl: './create-cr-fragment-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateContentRepositoryFragmentModalComponent implements IModalDialog, OnInit  {

    /** form instance */
    form: UntypedFormGroup;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityData: ContentRepositoryFragmentOperations,
        private entityExistsValidator: EntityExistsValidator<ContentRepositoryFragmentBO<Normalized>>,
    ) {
        entityExistsValidator.configure('contentRepositoryFragment', 'name');
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl(null , [Validators.required, Validators.minLength(2), this.entityExistsValidator.validate]),
        });
    }

    closeFn = (entityCreated: ContentRepositoryFragmentBO<Normalized>) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: ContentRepositoryFragmentBO<Normalized>) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * If user clicks to create a new dataSource
     */
     buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(createdEntity => {
                this.closeFn(createdEntity);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(): Promise<ContentRepositoryFragmentBO<Normalized>> {
        // assemble payload with conditional properties
        const request: ContentRepositoryFragmentCreateRequest = {
            name: this.form.value.name,
        };
        return this.entityData.create(request).toPromise();
    }

}
