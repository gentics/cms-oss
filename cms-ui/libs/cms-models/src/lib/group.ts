import { DefaultModelType, ModelType, Normalizable, NormalizableEntity, Raw } from './type-util';

/**
 * Represents a user group in the CMS.
 *
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_Group.html
 */
export interface Group<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {

    /** Group ID */
    id: number;

    /** Group name */
    name: string;

    /** Description */
    description: string;

    /** List of child groups */
    children?: Normalizable<T, Group<Raw>, number>[];

}
