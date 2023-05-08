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
