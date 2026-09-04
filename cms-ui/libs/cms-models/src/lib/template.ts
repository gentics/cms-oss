import { MarkupLanguage } from './markup-language';
import { InstancePermissionItem } from './permissions';
import { ObjectTag, TemplateTag } from './tag';
import { DefaultModelType, IndexByKey, ModelType, Normalizable, NormalizableEntity, Raw } from './type-util';
import { User } from './user';

export interface TemplateBase<T extends ModelType> extends NormalizableEntity<T> {

    /** Global ID of the template */
    globalId: string;

    /** Name of the template */
    name: string;

    /** Description of the template */
    description: string;

    /** Creator of the template */
    creator: Normalizable<T, User<Raw>, number>;

    /** Creation date of the template as a Unix timestamp */
    cdate: number;

    /** Last editor of the template */
    editor: Normalizable<T, User<Raw>, number>;

    /** Last Edit Date of the template as a Unix timestamp  */
    edate: number;

    /** True if the template is locked */
    locked: boolean;

    /** True if the template is inherited */
    inherited: boolean;

    /** True if this template is a master template */
    master: boolean;

    /** Master ID */
    masterId: number;

    /** The markup language of this template. */
    markupLanguage: MarkupLanguage<T>;

    /** Folder ID */
    folderId?: number;

    /** Folder path */
    path: string;

    /** Name of the node this template is inherited from */
    inheritedFrom: string;

    /** Name of the node, the master object belongs to */
    masterNode: string;

    /** The list of object tags for this template */
    objectTags: IndexByKey<ObjectTag>;

    /** Tags of the template */
    templateTags: IndexByKey<TemplateTag>;

    /** The source code of the template */
    source: string;

    /** Channel ID */
    channelId: number;

    /** Channelset ID */
    channelSetId: number;

    /** Item type. This is not returned by the API, but added when loaded. */
    type: 'template';

}

/** Data model as defined by backend. */
export interface Template<T extends ModelType = DefaultModelType> extends TemplateBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface TemplateBO<T extends ModelType = DefaultModelType> extends TemplateBase<T>, InstancePermissionItem {
    /** Internal ID of the object property definition */
    id: string;
}
