import { PASSWORD_VALIDATORS, getPatternEmail } from '@admin-ui/shared/utils';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors } from '@angular/forms';
import { GroupUserCreateRequest, Normalized, User } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { BehaviorSubject, Observable } from 'rxjs';
import { EntityExistsValidator } from '../../providers/entity-exists-validator/entity-exists-validator.service';
import { GroupDataService } from '../../providers/group-data/group-data.service';

@Component({
    selector: 'gtx-create-user-modal',
    templateUrl: './create-user-modal.component.html',
    styleUrls: ['./create-user-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateUserModalComponent extends BaseModal<User<Normalized>> implements OnInit {

    /** Current step (tab) of the entity creation wizzard */
    currentTab = String(1);

    /** form instance */
    form: UntypedFormGroup;

    /**
     * Group ids the new user will be added after creation.
     * This group assignment must be performed in two steps as a user can only be created in one group initially.
     */
    userGroupIds: number[] = [];

    /** Convenience getter for confirm username input */
    get login(): AbstractControl {
        return this.form.get('login');
    }
    /** Convenience getter for set password input */
    get password1(): AbstractControl {
        return this.form.get('password1');
    }
    /** Convenience getter for confirm password input */
    get password2(): AbstractControl {
        return this.form.get('password2');
    }

    /** Email regex pattern */
    get patternEmail(): string {
        return getPatternEmail();
    }

    /** search term for the table to search for */
    set searchTerm(v: string) {
        this._searchTerm.next(v);
    }
    get searchTerm(): string {
        return this._searchTerm.getValue();
    }
    get searchTerm$(): Observable<string> {
        return this._searchTerm.asObservable();
    }
    private _searchTerm = new BehaviorSubject<string>(null);

    constructor(
        private groupData: GroupDataService,
        private entityExistsValidator: EntityExistsValidator<User<Normalized>>,
    ) {
        super();
        entityExistsValidator.configure('user', 'login');
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            firstName: new UntypedFormControl( null ),
            lastName: new UntypedFormControl( null ),
            email: new UntypedFormControl( null ),
            login: new UntypedFormControl( null, this.entityExistsValidator.validate),
            description: new UntypedFormControl( null ),
            password1: new UntypedFormControl( null, PASSWORD_VALIDATORS ),
            password2: new UntypedFormControl( null, PASSWORD_VALIDATORS),
        }, this.passwordsDontMatch );
    }

    /** Get form validity state */
    formIsValid(): boolean {
        return this.form.valid;
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.formIsValid() && this.userGroupIds.length > 0;
    }

    /** Programmatic tab set */
    setActiveTab(index: string): void {
        this.currentTab = String(index);
    }

    /**
     * Returns TRUE if parameter index is index of active tab
     */
    tabIndexIsActive(index: number): boolean {
        return this.currentTab === String(index);
    }

    /**
     * Validator which checks that both passwords contain equal values.
     */
    passwordsDontMatch(control: UntypedFormControl): null | ValidationErrors {
        const valid = control.get('password1').value === control.get('password2').value;
        return valid ? null : { passwordsDontMatch: true };
    }

    /**
     * If user clicks to create a new user
     */
    buttonCreateUserClicked(): void {
        this.createUser()
            .then(entityCreated => this.closeFn(entityCreated));
    }

    private createUser(): Promise<User<Normalized>> {
        // assemble payload with conditional properties
        const user: GroupUserCreateRequest = {
            firstName: this.form.value.firstName,
            lastName: this.form.value.lastName,
            login: this.form.value.login,
            password: this.form.value.password1,
            ...(this.form.value.email && { email: this.form.value.email }),
            ...(this.form.value.description && { description: this.form.value.description }),
        };
        return this.groupData.createUser(user, this.userGroupIds).toPromise();
    }

}
