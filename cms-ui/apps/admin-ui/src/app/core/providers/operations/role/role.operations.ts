import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    DefaultModelType,
    I18nLanguage,
    I18nLanguageListResponse,
    ModelType,
    Raw,
    Role,
    RoleBO,
    RoleCreateResponse,
    RoleListOptions,
    RoleListResponse,
    RoleLoadResponse,
    RolePermissions,
    RolePermissionsLoadResponse,
    RolePermissionsUpdateResponse,
    RoleUpdateResponse,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { I18nNotificationService } from '../../i18n-notification';
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
            map((res: RoleListResponse) => {
                return res.items.map(item => this.mapRoleToRoleBO(item));
            }),
            tap((roles: RoleBO<Raw>[]) => {
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
            map((res: RoleLoadResponse) => res.role),
            map((item: Role<Raw>) => this.mapRoleToRoleBO(item)),
            tap((role: RoleBO<Raw>) => {
                this.entities.addEntity(this.entityIdentifier, role);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Create a role.
     */
    create(role: Partial<RoleBO<Raw>>, notification: boolean = true): Observable<RoleBO<Raw>> {
        /**
         * Currently there is no I18nOperations class and therefore also no dedicated state handling.
         * The i18n API calls are part of the LanguageOperations class.
         * Thus, we call the API directly ourselves for now.
         */
        return this.api.i18n.getAvailableUiLanguages().pipe(
            take(1),
            mergeMap((i18nLanguageListResponse: I18nLanguageListResponse): Observable<RoleCreateResponse> => {
                 return this.api.role.createRole(this.mapPartialRoleBOToPartialRole(
                     role,
                     i18nLanguageListResponse.items,
                 ));
            }),
            map((response: RoleCreateResponse) => response.role),
            map((role: Role<Raw>) => this.mapRoleToRoleBO(role)),
            tap((role: RoleBO<Raw>) => {
                this.entities.addEntity(this.entityIdentifier, role);
            }),
            tap((role: RoleBO<Raw>) => {
                if (notification) {
                    this.notification.show({
                        type: 'success',
                        message: 'shared.item_created',
                        translationParams: { name: role.name },
                    });
                }
            }),
            this.catchAndRethrowError(),
        )
    }

    /**
     * Change a single role
     */
    update(roleId: string, payload: Partial<RoleBO<Raw>>, notification: boolean = true): Observable<RoleBO<Raw>> {
        /**
         * Currently there is no I18nOperations class and therefore also no dedicated state handling.
         * The i18n API calls are part of the LanguageOperations class.
         * Thus, we call the API directly ourselves for now.
         */
        return this.api.i18n.getAvailableUiLanguages().pipe(
            take(1),
            mergeMap((i18nLanguageListResponse: I18nLanguageListResponse): Observable<RoleCreateResponse> => {
                 return this.api.role.updateRole(roleId, this.mapPartialRoleBOToPartialRole(
                     payload,
                     i18nLanguageListResponse.items,
                 ));
            }),
            map((res: RoleUpdateResponse) => res.role),
            map((role: Role<Raw>) => this.mapRoleToRoleBO(role)),
            // update state with server response
            tap((role: RoleBO<Raw>) => {
                this.entities.addEntity(this.entityIdentifier, role);
            }),
            // display toast notification
            tap((role: RoleBO<Raw>) => {
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
            }),
            // remove entity from state
            tap(() => this.entities.deleteEntities(this.entityIdentifier, [roleId])),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get permissions of a single role.
     */
    getPermissions(roleId: string): Observable<RolePermissions> {
        return this.api.role.getRolePermissions(roleId).pipe(
            map((res: RolePermissionsLoadResponse) => res.perm),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Change permissions of a single role
     */
    updatePermissions(roleId: string, payload: RolePermissions, notification: boolean = true): Observable<RolePermissions> {
        return this.api.role.updateRolePermissions(roleId, payload).pipe(
            map((res: RolePermissionsUpdateResponse) => res.perm),
            // display toast notification
            tap((_: RolePermissions) => {
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

    /*
     * Although roles can have a separate name and description for each UI language,
     * we assume in the frontend that they are not relevant for users.
     *
     * A Role contains the properties name and description. Both of them are objects itself,
     * where each key corresponds to a language code and each value to the respective translation.
     *
     * A RoleBO is the representation for a role we use in the frontend. The properties name and
     * description are strings.
     *
     * To avoid having to deal with mapping logic throughout the frontend, we map back and forth
     * only within this RoleOperations class. The necessary functions can be found below.
     */

    private mapRoleToRoleBO<T extends ModelType = DefaultModelType>(role: Role<T>): RoleBO<T> {
        // takes the first value of each i18n object
        let name = '';
        if (role.name && typeof role.name === 'object' && Object.values(role.name).length > 0) {
            name = Object.values(role.name)[0];
        }
        let description = '';
        if (role.description && typeof role.description === 'object' && Object.values(role.description).length > 0) {
            description = Object.values(role.description)[0];
        }
        return {
            id: `${role.id}`,
            name: name,
            description: description,
        };
    }

    /**
     * maps partial RoleBO to partial Role without id
     */
    private mapPartialRoleBOToPartialRole<T extends ModelType = DefaultModelType>(role: Partial<RoleBO<T>>, languages: I18nLanguage[]): Partial<Role<T>> {
        // languages is needed to know for which languages we need to create a property in the i18n object
        let name = {};
        let description = {};
        const nameToSet = role.name ? role.name : '';
        const descriptionToSet = role.description ? role.description : '';
        for (const language of languages) {
            name[language.code] = nameToSet;
            description[language.code] = descriptionToSet;
        }
        return {
            name: name,
            description: description,
        };
    }

}
