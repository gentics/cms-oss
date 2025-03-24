import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { UserActionPermissions } from '../user-action-permissions/user-action-permissions';

/**
 * A component that should be used with the `ActionAllowedDirective` has to implement either
 * the `DisableableComponent` or the `DisableableControlValueAccessor` interface,
 * i.e., the component must provide either a `setDisabledState()` method or a `disabled` property.
 */
 export interface DisableableComponent {
    disabled: boolean;
}

export interface PermissionsCheckResult {
    actionPerms: UserActionPermissions;
    granted: boolean;
}

export interface DeactivationConfig {
    hideElement: boolean;
    alwaysDisabled: boolean;
}

export enum PermissionsTreeType {
    CONTENT = 'content',
}

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
