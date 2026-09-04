import { Group, Normalized, PermissionsSet } from '@gentics/cms-models';

/**
 * Adds the group that, to which the permissions belong to the PermissionsSet.
 *
 * @note This data type is not part of the REST API.
 */
export interface PermissionsSetWithGroup extends PermissionsSet {
    group?: Group<Normalized>;
}
