import { EntityIdType, File, Folder, Form, Image, Node, Page, TemplateBO } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ContentItemBO } from './business-objects';

export enum SelectableType {
    FOLDER = 'folder',
    PAGE = 'page',
    FILE = 'file',
    IMAGE = 'image',
    FORM = 'form',
    TEMPLATE = 'template',
}

export type SelectableEntity = Node | Folder | Page | File | Image | Form | TemplateBO;

export interface PickableEntity {
    /** The element which has been picked/selected */
    entity: ContentItemBO;
    /** The entity type. Required on this level as well for the trable */
    type: string;
    /** The node-id from which node/channel it has been picked/selected from */
    nodeId: number;
}

/** Flag if the data should be retrieved from a certain devtool-package */
export const LOAD_FROM_PACKAGE = Symbol();
/** Flag if the data is being used in a package-list */
export const LOAD_FOR_PACKAGE_LIST = Symbol();
/** Flag if it should load the entities explicitly nested */
export const LOAD_NESTED = Symbol();
/** Flag if it should load the entities explicitly flattened */
export const LOAD_FLATTENED = Symbol();

export interface PackageEntityOperations<T_RAW> {
    getAllFromPackage(packageId: string, options?: any): Observable<T_RAW[]>;

    getFromPackage(packageId: string, entityId: EntityIdType): Observable<T_RAW>;
}
