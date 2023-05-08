import { AccessControlledType, PermissionsMapCollection } from '@gentics/cms-models';

import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils';

const PERMISSIONS: keyof AppState = 'permissions';

@ActionDeclaration(PERMISSIONS)
export class AddTypePermissionsMap {
    static readonly type = 'AddTypePermissionsMap';
    constructor(public type: AccessControlledType, public permissionsMapCollection: PermissionsMapCollection) {}
}

@ActionDeclaration(PERMISSIONS)
export class ClearAllPermissions {
    static readonly type = 'ClearAllPermissions';
}
