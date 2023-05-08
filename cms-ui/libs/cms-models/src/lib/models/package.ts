import { DefaultModelType, ModelType } from './type-util';

/** DevTools Package
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Package.html
 */
export interface Package<T extends ModelType = DefaultModelType> {
    /** Package name */
    name: string;
    /** Package description */
    description?: string;
    /** Number of constructs contained in the package */
    constructs?: number;
    /** Number of templates contained in the package */
    templates?: number;
    /** Number of datasources contained in the package */
    datasources?: number;
    /** Number of object properties contained in the package */
    objectProperties?: number;
    /** Number of ContentRepository Fragments contained in the package */
    crFragments?: number;
    /** Number of ContentRepositories contained in the package */
    contentRepositories?: number;
    /** Get set of sub packages */
    subPackages?: PackageBO<T>[];
}

/** DevTools Package
 * Data model as defined by frontend.
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Package.html
 */
export interface PackageBO<T extends ModelType = DefaultModelType> extends Package<T> {
    /** __Important__: Virtual property not present in REST API response */
    id: string;
}
