import { LanguageHandlerService, RoleOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Language, RoleBO, RoleCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-create-role-modal',
    templateUrl: './create-role-modal.component.html',
    styleUrls: [ './create-role-modal.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateRoleModalComponent extends BaseModal<RoleBO> implements OnInit {

    public supportedLanguages$: Observable<Language[]>;

    /** form instance */
    form: UntypedFormControl;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private roleOperations: RoleOperations,
        private languageHandler: LanguageHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({});
        this.supportedLanguages$ = this.languageHandler.getSupportedLanguages();
    }
    /**
     * If user clicks to create a new role
     */
    buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(roleCreated => {
                this.loading = false;
                this.closeFn(roleCreated);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(): Promise<RoleBO> {
        // assemble payload with conditional properties
        const role: RoleCreateRequest = {
            ...this.form.value,
        };
        return this.roleOperations.create(role).toPromise();
    }

}
