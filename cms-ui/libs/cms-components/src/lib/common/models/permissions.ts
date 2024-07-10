import {
    AccessControlledType,
    FolderItemType,
    GCMS_ROLE_PRIVILEGES_TO_GCMS_PERMISSIONS_MAP,
    GcmsPermission,
    GcmsRolePrivilege,
    PermissionsMapCollection,
} from '@gentics/cms-models';

/**
 * Represents the permissions of a user on a specific `AccessControlledType`.
 */
export interface TypePermissions {

    /** The AccessControlledType, to which these permissions apply. */
    readonly type: AccessControlledType;

    /**
     * @returns `true` if the user has the specified `permission`,
     * `false` if he does not have the permission, and
     * throws an error if the permission is not applicable to the type/instance.
     */
    hasPermission(permission: GcmsPermission): boolean;

}

/**
 * Represents the permissions of a user on a specific instance of an `AccessControlledType`.
 */
export interface InstancePermissions extends TypePermissions {

    /** The ID of the instance, to which these permissions apply. */
    readonly instanceId: number | string;

    /** The node ID where the instance is located. */
    readonly nodeId?: number;

}

/**
 * Represents the permissions of a user on a specific folder instance.
 * It allows querying role privileges as well.
 */
export interface FolderInstancePermissions extends InstancePermissions {

    /**
     * @returns `true` if the user has the specified `privilege` (or its equivalent `GcmsPermission`),
     * `false` if he does not have the privilege/permission, and
     * throws an error if the privilege/permission is not applicable.
     */
    hasPermissionOrRolePrivilege(itemType: FolderItemType, privilege: GcmsRolePrivilege): boolean;

    /**
     * @returns `true` if the user has the specified `privilege` (or its equivalent `GcmsPermission`)
     * for pages or for pages in a certain language (`languageCode`),
     * `false` if he does not have the permission.
     * It throws an error if the privilege/permission is not applicable.
     */
    hasPermissionOrRolePrivilege(itemType: 'page', privilege: GcmsRolePrivilege, languageCode?: string): boolean;

}

/**
 * Used to create `TypePermissions` and `InstancePermissions` objects.
 */
export interface PermissionsFactory {

    /**
     * Creates a `TypePermissions` object for the specified type and corresponding `PermissionsMapCollection`.
     */
    createPermissionsFromMaps(type: AccessControlledType, permissionsMaps: PermissionsMapCollection): TypePermissions;

    /**
     * Creates an `InstancePermissions` object for the specified instance of the type and corresponding `PermissionsMapCollection`.
     */
    createPermissionsFromMaps(
        type: AccessControlledType,
        permissionsMaps: PermissionsMapCollection,
        instanceId: number | string,
        nodeId?: number,
    ): InstancePermissions;

    /**
     * Creates a `FolderInstancePermissions` object for the specified instance of the folder and corresponding `PermissionsMapCollection`.
     */
    createPermissionsFromMaps(
        type: AccessControlledType.FOLDER, permissionsMaps: PermissionsMapCollection, instanceId: number | string, nodeId?: number,
    ): FolderInstancePermissions;

}

/**
 * Implementation of `TypePermissions`, which wraps a `PermissionsMapCollection` as received from the CMS.
 */
export class TypePermissionsImpl implements TypePermissions {

    constructor(
        public readonly type: AccessControlledType,
        protected permissionsMapCollection: PermissionsMapCollection,
    ) {}

    hasPermission(permission: GcmsPermission): boolean {
        if (!this.permissionsMapCollection?.permissions) {
            throw new Error(`The permission '${permission} is not applicable to the AccessControlledType '${this.type}'.`);
        }
        const permissionGranted = this.permissionsMapCollection.permissions[permission];
        if (permissionGranted !== undefined) {
            return permissionGranted;
        } else {
            throw new Error(`The permission '${permission} is not applicable to the AccessControlledType '${this.type}'.`);
        }
    }

}

/**
 * Implementation of `InstancePermissions`, which wraps a `PermissionsMapCollection` as received from the CMS.
 */
export class InstancePermissionsImpl extends TypePermissionsImpl implements InstancePermissions {

    constructor(
        type: AccessControlledType,
        permissionsMapCollection: PermissionsMapCollection,
        public readonly instanceId: number | string,
        public readonly nodeId?: number,
    ) {
        super(type, permissionsMapCollection);
    }

}

/**
 * Implementation of `FolderInstancePermissions`, which wraps a `PermissionsMapCollection` as received from the CMS.
 */
export class FolderInstancePermissionsImpl extends InstancePermissionsImpl implements FolderInstancePermissions {

    constructor(
        permissionsMapCollection: PermissionsMapCollection,
        readonly instanceId: number | string,
        readonly nodeId?: number,
    ) {
        super(AccessControlledType.FOLDER, permissionsMapCollection, instanceId, nodeId);
    }

    hasPermissionOrRolePrivilege(itemType: FolderItemType, privilege: GcmsRolePrivilege, languageCode?: string): boolean {
        if (itemType !== 'page' && (privilege === GcmsRolePrivilege.publishpages || privilege === GcmsRolePrivilege.translatepages)) {
            throw new Error(`The privilege '${privilege} is not applicable to '${itemType}'`);
        }

        const permission = GCMS_ROLE_PRIVILEGES_TO_GCMS_PERMISSIONS_MAP[privilege];
        if (permission) {
            // If the GcmsPermission is granted, we do not need to check the role privileges.
            if (this.hasPermission(permission)) {
                return true;
            }
        }

        switch (itemType) {
            case 'page':
                return this.hasRolePrivilegeForPages(privilege, languageCode);
            case 'file':
            case 'image':
                return this.hasRolePrivilegeForFiles(privilege);
            default:
                return false;
        }
    }

    private hasRolePrivilegeForFiles(privilege: GcmsRolePrivilege): boolean {
        return !!(this.permissionsMapCollection.rolePermissions && this.permissionsMapCollection.rolePermissions.file[privilege]);
    }

    private hasRolePrivilegeForPages(privilege: GcmsRolePrivilege, langCode?: string): boolean {
        if (!this.permissionsMapCollection.rolePermissions) {
            return false;
        }
        if (this.permissionsMapCollection.rolePermissions.page[privilege]) {
            return true;
        }
        if (langCode && this.permissionsMapCollection.rolePermissions.pageLanguages) {
            const langPrivileges = this.permissionsMapCollection.rolePermissions.pageLanguages[langCode];
            return !!(langPrivileges && langPrivileges[privilege]);
        }
        return false;
    }

}


/**
 * The default `PermissionsFactory`.
 */
export class DefaultPermissionsFactory implements PermissionsFactory {

    createPermissionsFromMaps(type: AccessControlledType, permissionsMaps: PermissionsMapCollection): TypePermissions;
    createPermissionsFromMaps(
        type: AccessControlledType,
        permissionsMaps: PermissionsMapCollection,
        instanceId: number | string,
        nodeId?: number): InstancePermissions;
    createPermissionsFromMaps(
        type: AccessControlledType.FOLDER, permissionsMaps: PermissionsMapCollection, instanceId: number | string, nodeId?: number,
    ): FolderInstancePermissions;
    createPermissionsFromMaps(
        type: AccessControlledType, permissionsMaps: PermissionsMapCollection, instanceId?: number | string, nodeId?: number,
    ): TypePermissions | InstancePermissions {
        if (instanceId !== undefined) {
            if (type !== AccessControlledType.FOLDER) {
                return new InstancePermissionsImpl(type, permissionsMaps, instanceId, nodeId);
            } else {
                return new FolderInstancePermissionsImpl(permissionsMaps, instanceId, nodeId);
            }
        } else {
            return new TypePermissionsImpl(type, permissionsMaps);
        }
    }

}

/**
 * Utility class, where `hasPermission()` will always return a pre-defined value.
 */
export class UniformTypePermissions implements TypePermissions {

    constructor(
        public readonly type: AccessControlledType,
        public readonly permissionsGranted: boolean,
    ) {}

    hasPermission(permission: GcmsPermission): boolean {
        return this.permissionsGranted;
    }

}

/**
 * Utility class, where `hasPermission()` will always return a pre-defined value.
 */
export class UniformInstancePermissions extends UniformTypePermissions implements InstancePermissions {
    constructor(
        type: AccessControlledType,
        permissionsGranted: boolean,
        public readonly instanceId: number | string,
        public readonly nodeId?: number,
    ) {
        super(type, permissionsGranted);
    }
}
