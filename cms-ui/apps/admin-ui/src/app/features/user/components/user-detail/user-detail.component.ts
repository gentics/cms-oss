import { createFormSaveDisabledTracker, FormGroupTabHandle, FormTabHandle, NULL_FORM_TAB_HANDLE, UserDetailTabs } from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    EditorTabTrackerService,
    PermissionsService,
    ResolveBreadcrumbFn,
    UserOperations,
} from '@admin-ui/core';
import { ChangePasswordModalComponent } from '@admin-ui/core/components/change-password-modal/change-password-modal.component';
import { ErrorHandler } from '@admin-ui/core/providers/error-handler/error-handler.service';
import { BaseDetailComponent, getPatternEmail, UserDataService, UserTableLoaderService } from '@admin-ui/shared';
import { AppStateService, UIStateModel } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    Type,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    AccessControlledType,
    GcmsPermission,
    NormalizableEntityType,
    Normalized,
    Raw,
    User,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { combineLatest, Observable, of } from 'rxjs';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { detailLoading } from '../../../../common/utils/rxjs-loading-operators/detail-loading.operator';

// *************************************************************************************************
/**
 * # UserDetailComponent
 * Display and edit entity user detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-user-detail',
    templateUrl: './user-detail.component.html',
    styleUrls: [ 'user-detail.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDetailComponent extends BaseDetailComponent<'user', UserOperations> implements OnInit {

    public readonly UserDetailTabs = UserDetailTabs;

    entityIdentifier: NormalizableEntityType = 'user';

    /** current entity value */
    currentEntity: User<Raw>;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    fgPropertiesSaveDisabled$: Observable<boolean>;

    /** Email regex pattern */
    get patternEmail(): string {
        return getPatternEmail();
    }

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.login || this.currentEntity.login === '';
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    /** TRUE if logged-in user is allowed to read entity `group` */
    permissionGroupsRead$: Observable<boolean>;

    activeTabId$: Observable<string>;

    private tabHandles: Record<UserDetailTabs, FormTabHandle>;

    constructor(
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        userData: UserDataService,
        changeDetectorRef: ChangeDetectorRef,
        private userOperations: UserOperations,
        private errorHandler: ErrorHandler,
        private modalService: ModalService,
        private permissionsService: PermissionsService,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: UserTableLoaderService,
    ) {
        super(
            route,
            router,
            appState,
            userData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const user = appState.now.entity.user[Number(route.params.id)];
        return of(user ? { title: user.login, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init forms
        this.initForms();

        // assign values and validation of current entity
        combineLatest([
            this.currentEntity$,
            this.userState$.pipe(
                switchMap((userState: UIStateModel) => this.userOperations.getUserGroupsWithPermissions(userState.focusEntityId as number)),
            ),
        ]).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(([currentEntity, userGroups]) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.fgPropertiesUpdate(currentEntity);
            this.changeDetectorRef.markForCheck();
            const commonPermissions = Object.values(userGroups.perms).reduce((accumulator, currentList) => {
                return accumulator.filter(permission => currentList.includes(permission));
            });
            if (this.currentEntity.id !== this.appState.now.auth.currentUserId && !commonPermissions.includes(GcmsPermission.EDIT)) {
                this.fgProperties.disable();
            } else {
                this.fgProperties.enable();
            }
        });

        this.permissionGroupsRead$ = this.permissionsService.getPermissions(AccessControlledType.GROUP_ADMIN).pipe(
            map(typePermissions => typePermissions.hasPermission(GcmsPermission.READ)),
        );

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    /**
     * If user clicks to set new password for current user
     */
    btnSetPasswordClick(): void {
        this.modalService.fromComponent(
            ChangePasswordModalComponent,
            { closeOnOverlayClick: true },
            { userId: this.currentEntity.id },
        ).then(modal => modal.open())
            .catch(this.errorHandler.catch);
    }

    btnSavePropertiesOnClick(): void {
        this.updateUser();
    }

    /**
     * Requests changes of user by id to CMS
     */
    updateUser(): Promise<void> {
        // assemble payload with conditional properties
        const user: User<Raw> = {
            id: this.currentEntity.id,
            ...(this.fgProperties.value.firstName && { firstName: this.fgProperties.value.firstName }),
            ...(this.fgProperties.value.lastName && { lastName: this.fgProperties.value.lastName }),
            ...(this.fgProperties.value.email && { email: this.fgProperties.value.email }),
            ...(this.fgProperties.value.login && { login: this.fgProperties.value.login }),
            ...(this.fgProperties.value.description && { description: this.fgProperties.value.description }),
        };
        return this.userOperations.update(user.id, user).pipe(
            detailLoading(this.appState),
            tap((updatedUser: User<Raw>) => this.currentEntity = updatedUser),
            map(() => {
                this.fgProperties.markAsPristine();
                this.tableLoader.reload();
            }),
        ).toPromise();
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            firstName: new UntypedFormControl(''),
            lastName: new UntypedFormControl(''),
            email: new UntypedFormControl(''),
            login: new UntypedFormControl(''),
            description: new UntypedFormControl(''),
        });

        this.fgPropertiesSaveDisabled$ = createFormSaveDisabledTracker(this.fgProperties);
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(user: User<Normalized | Raw>): void {
        this.fgProperties.setValue({
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            login: user.login,
            description: user.description,
        });
        this.fgProperties.markAsPristine();
    }

    private initForms(): void {
        this.fgPropertiesInit();

        this.tabHandles = {
            [UserDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateUser(),
            }),
            [UserDetailTabs.USER_GROUPS]: NULL_FORM_TAB_HANDLE,
            [UserDetailTabs.PERMISSIONS_ADMIN]: NULL_FORM_TAB_HANDLE,
            [UserDetailTabs.PEROMSSIONS_CONTENT]: NULL_FORM_TAB_HANDLE,
        };
    }

}
