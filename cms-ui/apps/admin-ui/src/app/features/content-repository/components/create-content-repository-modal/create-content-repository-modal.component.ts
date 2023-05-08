import { ContentRepositoryOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ContentRepositoryBO, ContentRepositoryCreateRequest } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-content-repository-modal',
    templateUrl: './create-content-repository-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateContentRepositoryModalComponent implements IModalDialog, OnInit {

    /** form instance */
    form: UntypedFormControl;

    isValid: boolean;

    constructor(
        private contentRepositoryOperations: ContentRepositoryOperations,
    ) { }

    ngOnInit(): void {
        const payload: ContentRepositoryCreateRequest = {
            name: '',
            crType: null,
            dbType: '',
            username: '',
            password: '',
            usePassword: false,
            url: '',
            basepath: '',
            instantPublishing: false,
            languageInformation: false,
            permissionInformation: false,
            permissionProperty: '',
            defaultPermission: '',
            diffDelete: false,
            elasticsearch: null,
            projectPerNode: false,
        };
        // instantiate form
        this.form = new UntypedFormControl(payload);
    }

    closeFn = (entityCreated: ContentRepositoryBO) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: ContentRepositoryBO) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /**
     * If user clicks to create a new contentRepository
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(contentRepositoryCreated => this.closeFn(contentRepositoryCreated));
    }

    private createEntity(): Promise<ContentRepositoryBO> {
        return this.contentRepositoryOperations.create(this.form.value).toPromise();
    }

}
