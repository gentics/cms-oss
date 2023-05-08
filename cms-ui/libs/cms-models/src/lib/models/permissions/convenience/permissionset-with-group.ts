import { PermissionsSet } from '..';

import { Group, Normalized } from '../..';

/**
 * Adds the group that, to which the permissions belong to the PermissionsSet.
 *
 * @note This data type is not part of the REST API.
 */
export interface PermissionsSetWithGroup extends PermissionsSet {
    group?: Group<Normalized>;
}
