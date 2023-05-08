/**
 * Determines whether a `Role` is assigned to a group.
 */
export interface RoleAssignment {

    /** Role ID. */
    id: number;

    /** Role label (translated by CMS). */
    label: string;

    /** Role description (translated by CMS). */
    description: string;

    /** `true` if role is assigned, `false` if not */
    value: boolean;

    /** `true` if the role assignment can be edited by the current user. */
    editable: boolean;

}
