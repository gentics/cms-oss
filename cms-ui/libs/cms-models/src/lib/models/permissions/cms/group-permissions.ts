import { Group } from '../../group';
import { AccessControlledType, GcmsPermission } from './permissions';
import { RoleAssignment } from './roles';

/**
 * Indicate if a group has a certain `GcmsPermission`.
 *
 * This can be used for permissions on an `AccessControlledType` or on an instance of a type.
 */
export interface PermissionInfo {

    /** The `GcmsPermission` that is represented by this instance. */
    type: GcmsPermission;

    /** The human-readable label for this permission (translated by the CMS). */
    label: string;

    /** The human-readable category for this permission (translated by the CMS). */
    category: string;

    /** `true` if the permission is granted, otherwise `false` */
    value: boolean;

    /** `true` if the permission can be edited by the current user. */
    editable: boolean;

}

/**
 * Base interface for types that permissions and role infos for a particular group
 * on an `AccessControlledType` or an instance of it.
 */
export interface PermissionsAndRoles {

    /** All permissions that apply to the `AccessControlledType` or its instance. */
    perms: PermissionInfo[];

    /**
     * The roles that are assigned to the group in the folder instance, for
     * which this info has been loaded.
     *
     * This is only set for instances of `AccessControlledType.folder`, since
     * roles can only be assigned to a group in a folder.
     */
    roles?: RoleAssignment[];

}

/**
 * Contains all permissions applicable to a certain `AccessControlledType` or
 * a particular instance of it (if `id` is set) and whether the group,
 * for which the permissions were requested, has each permission.
 */
export interface PermissionsSet extends PermissionsAndRoles {

    /** The `AccessControlledType`, to which the permissions apply. */
    type: AccessControlledType;

    /**
     * If this `PermissionsSet` refers to a particular type instance, this
     * is set to the ID of that instance. If this is not set, the permissions
     * apply to the `AccessControlledType` itself.
     */
    id?: number;

    /**
     * If this `PermissionsSet` refers to a particular type instance that is scoped
     * to a node/channel, this is set to the ID of the node/channel.
     */
    channelId?: number;

    /** The human-readable label for the `AccessControlledType` or its instance (translated by the CMS). */
    label: string;

    /** `true` if this type or instance has child permissions. */
    children: boolean;

    /** `true` if the permissions can be edited by the current user. */
    editable: boolean;

}
