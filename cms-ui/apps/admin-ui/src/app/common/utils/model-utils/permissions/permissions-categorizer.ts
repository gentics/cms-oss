import { GcmsPermission, IndexByKey, PermissionInfo } from '@gentics/cms-models';
import { groupBy as _groupBy } from 'lodash';

export const COMMON_CATEGORY = 'shared.common_perms';
export const SPECIFIC_CATEGORY = 'shared.specific_perms';

export const COMMON_PERMS = new Set<GcmsPermission>([
    GcmsPermission.READ,
    GcmsPermission.SET_PERMISSION,
]);

export interface CategoryInfo {
    id: string;
    label: string;
}

export interface PermissionsCategorizer {

    /**
     * Groups the PermissionInfo objects by the IDs of their categories and adds
     * the categories to the list of categories retrievable via
     */
    categorizePermissions(perms: PermissionInfo[]): IndexByKey<PermissionInfo[]>;

    /**
     * @returns The number of known categories.
     */
    getKnownCategoriesCount(): number;

    /**
     * @returns All known categories as an array.
     */
    getKnownCategories(): CategoryInfo[];

}

abstract class PermissionsCategorizerBase implements PermissionsCategorizer {

    /** Maintains a list of all permission categories known to this instance. */
    protected categoriesMap: Map<string, CategoryInfo> = new Map();

    abstract categorizePermissions(perms: PermissionInfo[]): IndexByKey<PermissionInfo[]>;

    getKnownCategoriesCount(): number {
        return this.categoriesMap.size;
    }

    getKnownCategories(): CategoryInfo[] {
        return Array.from(this.categoriesMap.values());
    }

}

export class PermissionsCategorizerByCategoryId extends PermissionsCategorizerBase {

    categorizePermissions(perms: PermissionInfo[]): IndexByKey<PermissionInfo[]> {
        return _groupBy(perms, permInfo => this.determineCategoryId(permInfo));
    }

    private determineCategoryId(permInfo: PermissionInfo): string {
        const categoryId = permInfo.category || COMMON_CATEGORY;
        if (!this.categoriesMap.get(categoryId)) {
            // If the backend ever supplies translated category labels in a different property, we can store them here.
            this.categoriesMap.set(categoryId, { id: categoryId, label: categoryId });
        }
        return categoryId;
    }

}

export class PermissionsCategorizerByPermType extends PermissionsCategorizerBase {

    constructor() {
        super();
        this.categoriesMap.set(COMMON_CATEGORY, { id: COMMON_CATEGORY, label: COMMON_CATEGORY });
        this.categoriesMap.set(SPECIFIC_CATEGORY, { id: SPECIFIC_CATEGORY, label: SPECIFIC_CATEGORY });
    }

    categorizePermissions(perms: PermissionInfo[]): IndexByKey<PermissionInfo[]> {
        return _groupBy(
            perms,
            permInfo => COMMON_PERMS.has(permInfo.type) ? COMMON_CATEGORY : SPECIFIC_CATEGORY,
        );
    }

}
