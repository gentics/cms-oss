import {
    COMMON_CATEGORY,
    COMMON_PERMS,
    PermissionsCategorizer,
    PermissionsCategorizerByCategoryId,
    PermissionsCategorizerByPermType,
    SPECIFIC_CATEGORY,
} from './permissions-categorizer';

export class PermissionsUtils {

    static readonly COMMON_CATEGORY_LABEL = COMMON_CATEGORY;
    static readonly SPECIFIC_CATEGORY_LABEL = SPECIFIC_CATEGORY;
    static readonly COMMON_PERMS = COMMON_PERMS;

    static createCategorizerByCategoryId(): PermissionsCategorizer {
        return new PermissionsCategorizerByCategoryId();
    }

    static createCategorizerByPermType(): PermissionsCategorizer {
        return new PermissionsCategorizerByPermType();
    }

}
