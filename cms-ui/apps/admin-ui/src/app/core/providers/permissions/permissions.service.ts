import { MultiModuleUserActionPermissions, USER_ACTION_PERMISSIONS, UserActionPermissions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { AddTypePermissionsMap } from '@admin-ui/state/permissions/permissions.actions';
import { Inject, Injectable } from '@angular/core';
import {
    DefaultPermissionsFactory,
    FolderInstancePermissions,
    InstancePermissions,
    TypePermissions,
    UniformInstancePermissions,
    UniformTypePermissions,
} from '@gentics/cms-components';
import {
    AccessControlledType,
    EntityIdType,
    GcmsPermission,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, ReplaySubject, combineLatest, of } from 'rxjs';
import {
    catchError,
    map,
    switchMap,
    switchMapTo,
    tap,
} from 'rxjs/operators';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';
import { ErrorHandler } from '../error-handler';

/**
 * Describes the permissions on a specific `AccessControlledType` that are needed for a user to be able to access something.
 */
export interface RequiredTypePermissions {
    /** The type, for which the permissions should be checked. */
    type: AccessControlledType;

    /** The permission(s) that are required. */
    permissions: GcmsPermission | GcmsPermission[];
}

/**
 * Describes the permissions on a specific `AccessControlledType` instance that are needed for a user to be able to access something.
 */
export interface RequiredInstancePermissions extends RequiredTypePermissions {
    /** The ID of the type instance, on which the permissions need to be granted. */
    instanceId: number | string;

    /** The ID of the node, where the instance, on which the permissions need to be granted, is located in. */
    nodeId?: number;
}

/**
 * Describes the permissions on an `AccessControlledType` or an instance of it, which are needed for a user to be able to access something.
 */
export type RequiredPermissions = RequiredTypePermissions | RequiredInstancePermissions;

/**
 * A service that provides a central way to check for user permissions.
 *
 * This service also clears the permissions in the AppState when the user logs out.
 */
@Injectable()
export class PermissionsService extends ServiceBase {

    protected permFactory = new DefaultPermissionsFactory();
    protected readonly instanceCache: { [key: string]: ReplaySubject<InstancePermissions> } = {};

    constructor(
        private api: GcmsApi,
        private appState: AppStateService,
        private errorHandler: ErrorHandler,
        @Inject(USER_ACTION_PERMISSIONS) private userActionPermissions: MultiModuleUserActionPermissions,
    ) {
        super();
    }

    public getUserActionPermsForId(actionId: string): UserActionPermissions {
        if (!actionId) {
            return null;
        }

        const parts = actionId.split('.');
        if (parts.length !== 2) {
            throw new Error(`Malformed user action ID provided to gtxActionAllowed directive: '${actionId}'.` +
                'Make sure that you use the format \'<module>.actionId');
        }

        const module = this.userActionPermissions[parts[0]];
        if (!module) {
            throw new Error(`User Action module '${parts[0]}' does not exist.`);
        }
        const reqPerms = module[parts[1]];
        if (!reqPerms) {
            throw new Error(`User Action '${parts[1]}' does not exist within module '${parts[0]}'.`);
        }
        return reqPerms;
    }

    /**
     * Gets the `TypePermissions` for the specified `type`.
     */
    getPermissions(type: AccessControlledType): Observable<TypePermissions>;
    /**
     * Gets the `InstancePermissions` for the specified `type`, `instanceId`, and (optionally) `nodeId`.
     */
    getPermissions(type: AccessControlledType, instanceId: EntityIdType, nodeId?: number): Observable<InstancePermissions>;
    /**
     * Gets the `FolderInstancePermissions` for the specified `instanceId`, and (optionally) `nodeId`.
     */
    getPermissions(type: AccessControlledType.FOLDER, instanceId: EntityIdType, nodeId?: number): Observable<FolderInstancePermissions>;
    /**
     * Gets the `TypePermissions` for the specified `type` or,
     * if `instanceId` is set, the `InstancePermissions` for the specified `type`, `instanceId`, and (optionally) `nodeId`.
     *
     * This is a convenience method to avoid having to select between `getTypePermissions()` and `getInstancePermissions()` manually.
     */
    getPermissions(
        type: AccessControlledType,
        instanceId?: EntityIdType,
        nodeId?: number,
        forceRefresh: boolean = false,
    ): Observable<TypePermissions> {
        if (typeof instanceId === 'number' || typeof instanceId === 'string') {
            return this.getInstancePermissions(type, instanceId, nodeId, forceRefresh);
        } else {
            return this.getTypePermissions(type, forceRefresh);
        }
    }

    /**
     * Gets the `TypePermissions` for the specified `type`.
     *
     * If the permissions are not yet in the AppState or if `forceRefresh` is true,
     * they are fetched from the CMS and then stored in the AppState.
     * Whenever the permissions in the AppState change, the returned observable emits again.
     */
    getTypePermissions(type: AccessControlledType, forceRefresh: boolean = false): Observable<TypePermissions> {
        const permsFromAppState$ = this.appState.select(state => state.permissions.types[type]).pipe(
            map(permissionsMap => {
                if (permissionsMap && (permissionsMap.permissions || permissionsMap.rolePermissions)) {
                    return this.permFactory.createPermissionsFromMaps(type, permissionsMap);
                } else {
                    // If permissionsMap is not set, there was an error fetching the permissions.
                    return new UniformTypePermissions(type, false);
                }
            }),
        );
        let ret$ = permsFromAppState$;

        if (forceRefresh || !this.appState.now.permissions.types[type]) {
            ret$ = this.api.permissions.getPermissionsForType(type).pipe(
                switchMap(response => {
                    const permissionsMap = response.permissionsMap;
                    return this.appState.dispatch(new AddTypePermissionsMap(type, permissionsMap));
                }),
                catchError(error => {
                    this.errorHandler.catch(error, { notification: true });
                    return of(null);
                }),
                switchMapTo(permsFromAppState$),
            );
        }

        return ret$;
    }

    /**
     * Gets the `InstancePermissions` for the specified `type`, `instanceId`, and (optionally) `nodeId`.
     */
    getInstancePermissions(
        type: AccessControlledType,
        instanceId: EntityIdType,
        nodeId?: number,
        forceRefresh?: boolean,
    ): Observable<InstancePermissions>;
    getInstancePermissions(
        type: AccessControlledType.FOLDER,
        instanceId: EntityIdType,
        nodeId?: number,
        forceRefresh?: boolean,
    ): Observable<FolderInstancePermissions>;
    getInstancePermissions(
        type: AccessControlledType,
        instanceId: EntityIdType,
        nodeId?: number,
        forceRefresh: boolean = false,
    ): Observable<InstancePermissions> {
        const cacheKey = `${type}/${instanceId}${nodeId ? `/${nodeId}` : ''}`;
        if (!forceRefresh && this.instanceCache[cacheKey]) {
            return (this.instanceCache[cacheKey]).asObservable();
        }

        if (type === 'template') {
            return this.getTemplatePermissions(instanceId).pipe(
                tap(perm => {
                    const sub = new ReplaySubject<InstancePermissions>();
                    this.instanceCache[cacheKey] = sub;
                    sub.next(perm);
                }),
            );
        }

        return this.api.permissions.getPermissionsForInstance(type, instanceId, nodeId).pipe(
            map(response => response.permissionsMap),
            map(permissionsMap => this.permFactory.createPermissionsFromMaps(type, permissionsMap, instanceId, nodeId)),
            tap(perm => {
                const sub = new ReplaySubject<InstancePermissions>();
                this.instanceCache[cacheKey] = sub;
                sub.next(perm);
            }),
            catchError(error => {
                this.errorHandler.catch(error, { notification: true });
                return of(new UniformInstancePermissions(type, false, instanceId, nodeId));
            }),
        );
    }

    getTemplatePermissions(folderId: EntityIdType): Observable<InstancePermissions> {
        return combineLatest([
            this.api.permissions.getTemplateViewPermissions(folderId),
            this.api.permissions.getTemplateEditPermissions(folderId),
            this.api.permissions.getTemplateDeletePermissions(folderId),
        ]).pipe(
            map(([viewPerms, editPerms, deletePerms]) => [viewPerms.granted, editPerms.granted, deletePerms.granted]),
            map(([canView, canEdit, canDelete]) => {
                return {
                    type: AccessControlledType.TEMPLATE,
                    hasPermission: (perm) => {
                        switch (perm) {
                            case GcmsPermission.READ:
                                return canView;
                            case GcmsPermission.UPDATE:
                                return canEdit;
                            case GcmsPermission.DELETE:
                                return canDelete;
                            default:
                                return false;
                        }
                    },
                    instanceId: folderId,
                    nodeId: null,
                };
            }),
        );
    }

    storePermissions(
        type: AccessControlledType,
        instanceId: EntityIdType,
        nodeIdOrPermissions: number | GcmsPermission | GcmsPermission[],
        ...permissions: GcmsPermission[]
    ): InstancePermissions {
        permissions = permissions || [];
        let nodeId = -1;

        if (typeof nodeIdOrPermissions !== 'number') {
            permissions.push(...Array.isArray(nodeIdOrPermissions) ? nodeIdOrPermissions : [nodeIdOrPermissions]);
        } else {
            nodeId = nodeIdOrPermissions;
        }

        const cacheKey = `${type}/${instanceId}${nodeId > 0 ? `/${nodeId}` : ''}`;
        const constructed: InstancePermissions = {
            type,
            instanceId,
            nodeId,
            hasPermission: (permToCheck: GcmsPermission) => permissions.includes(permToCheck),
        };

        const sub = new ReplaySubject<InstancePermissions>();
        this.instanceCache[cacheKey] = sub;
        sub.next(constructed);

        return constructed;
    }

    /**
     * Checks a single or a set of required type of instance permissions.
     *
     * @returns An observable that emits `true` if all required permissions have been granted and `false` otherwise.
     */
    checkPermissions(requiredPermissions: RequiredPermissions | RequiredPermissions[]): Observable<boolean> {
        // empty array will return true
        if (Array.isArray(requiredPermissions) && requiredPermissions.length === 0) {
            return of(true);
        }
        const reqPermissions = Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions];
        const permChecks: Observable<boolean>[] = reqPermissions.map(reqPerm => this.executePermissionsCheck(reqPerm));
        return combineLatest(permChecks).pipe(
            map(results => results.every(permGranted => permGranted)),
        );
    }

    private executePermissionsCheck(reqPermissions: RequiredPermissions): Observable<boolean> {
        const permsToCheck = Array.isArray(reqPermissions.permissions) ? reqPermissions.permissions : [reqPermissions.permissions];
        let actualPermissions$: Observable<TypePermissions>;

        if (typeof (reqPermissions as RequiredInstancePermissions).instanceId === 'number' ||
            typeof (reqPermissions as RequiredInstancePermissions).instanceId === 'string') {
            const reqInstancePerms = reqPermissions as RequiredInstancePermissions;
            actualPermissions$ = this.getInstancePermissions(reqInstancePerms.type, reqInstancePerms.instanceId, reqInstancePerms.nodeId);
        } else {
            actualPermissions$ = this.getTypePermissions(reqPermissions.type);
        }

        return actualPermissions$.pipe(
            map(actualPermissions => permsToCheck.every(perm => actualPermissions.hasPermission(perm))),
        );
    }

}
