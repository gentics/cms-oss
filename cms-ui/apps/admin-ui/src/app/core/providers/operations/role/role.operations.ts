import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    DefaultModelType,
    ModelType,
    Raw,
    Role,
    RoleBO,
    RoleCreateRequest,
    RoleListOptions,
    RolePermissions,
    RoleUpdateRequest,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

/**
 * Operations on Roles.
 *
 * All operations that modify something first make the change using the REST API
 * and then update the AppState accordingly.
 */
@Injectable()
export class RoleOperations extends ExtendedEntityOperationsBase<'role'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
        private notification: I18nNotificationService,
        private appState: AppStateService,
    ) {
        super(injector, 'role');
    }

    /**
     * Get a list of all roles and adds them to the AppState.
     */
    getAll(options?: RoleListOptions): Observable<RoleBO<ModelType.Raw>[]> {
        return this.api.role.getRoles(options).pipe(
            map((res) => {
                return res.items.map((item) => this.mapToBusinessObject(item));
            }),
            tap((roles) => {
                this.entities.addEntities(this.entityIdentifier, roles);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single role and add it to the AppState.
     */
    get(roleId: string): Observable<RoleBO<ModelType.Raw>> {
        return this.api.role.getRole(roleId).pipe(
            map((res) => this.mapToBusinessObject(res.role)),
            tap((role) => {
                this.entities.addEntity(this.entityIdentifier, role);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a role.
     */
    create(role: RoleCreateRequest, notification: boolean = true): Observable<RoleBO<Raw>> {
        /**
         * Currently there is no I18nOperations class and therefore also no dedicated state handling.
         * The i18n API calls are part of the LanguageOperations class.
         * Thus, we call the API directly ourselves for now.
         */
        return this.api.role.createRole(role).pipe(
            map((res) => this.mapToBusinessObject(res.role)),
            tap((role) => {
                this.entities.addEntity(this.entityIdentifier, role);

                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: role.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change a single role
     */
    update(roleId: number | string, payload: RoleUpdateRequest, notification: boolean = true): Observable<RoleBO<Raw>> {
        /**
         * Currently there is no I18nOperations class and therefore also no dedicated state handling.
         * The i18n API calls are part of the LanguageOperations class.
         * Thus, we call the API directly ourselves for now.
         */
        return this.api.role.updateRole(roleId, payload).pipe(
            map((res) => this.mapToBusinessObject(res.role)),
            // display toast notification
            tap((role) => {
                // update state with server response
                this.entities.addEntity(this.entityIdentifier, role);

                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_updated',
                        translationParams: { name: role.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Delete a single dataSource
     */
    delete(roleId: string | number, notification: boolean = true): Observable<Response | void> {
        const roleToBeDeleted = this.appState.now.entity.role[roleId];

        return this.api.role.deleteRole(roleId).pipe(
            // display toast notification
            tap(() => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_singular_deleted',
                        translationParams: { name: roleToBeDeleted.name },
                    });
                }
                // remove entity from state
                this.entities.deleteEntities(this.entityIdentifier, [roleId]);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get permissions of a single role.
     */
    getPermissions(roleId: string): Observable<RolePermissions> {
        return this.api.role.getRolePermissions(roleId).pipe(
            map((res) => res.perm),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change permissions of a single role
     */
    updatePermissions(roleId: string, payload: RolePermissions, notification: boolean = true): Observable<RolePermissions> {
        return this.api.role.updateRolePermissions(roleId, payload).pipe(
            map((res) => res.perm),
            // display toast notification
            tap(() => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'role.permissions_updated',
                    });
                }
            }),
            this.catchAndRethrowError(),
        );
    }

    private mapToBusinessObject<T extends ModelType = DefaultModelType>(role: Role<T>): RoleBO<T> {
        return {
            ...role,
            id: String(role.id),
        };
    }
}
