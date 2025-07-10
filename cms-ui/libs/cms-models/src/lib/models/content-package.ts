import { Form } from './cms-form';
import { NodeFeature } from './feature';
import { File } from './file';
import { Folder } from './folder';
import { Image } from './image';
import { Node } from './node';
import { Page } from './page';
import { Tag } from './tag';
import {
    BooleanTagPartProperty,
    FileTagPartProperty,
    FolderTagPartProperty,
    FormTagPartProperty,
    ImageTagPartProperty,
    ListTagPartProperty,
    NodeTagPartProperty,
    PageTagPartProperty,
    PageTagTagPartProperty,
    SelectTagPartProperty,
    StringTagPartProperty,
    TagPartProperty,
    TemplateTagTagPartProperty,
} from './tag-part';
import { Overview } from './tag-property-values';
import { DefaultModelType, ModelType, NormalizableEntity } from './type-util';

export interface ContentPackageSyncProgress {
    done: number;
    total: number;
    started: number;
    finished: number;
}

export interface ContentPackageImport {
    running: boolean;
    progress?: ContentPackageSyncProgress;
}

export interface ContentPackage<T extends ModelType = DefaultModelType> extends NormalizableEntity<T> {
    /** The global-id of the package (only available when imported) */
    globalId?: string;
    /** The timestamp when the package was imported the last time */
    timestamp?: number;

    /** The name/identifier of the package */
    name: string;
    /** The description of the package */
    description?: string;

    /** How many files are contained in the package */
    files?: number;
    /** How many folders are contained in the package */
    folders?: number;
    /** How many forms are contained in the package */
    forms?: number;
    /** How many images are contained in the package */
    images?: number;
    /** How many pages are contained in the package */
    pages?: number;

    /** Information about the latest import */
    import?: ContentPackageImport;
}

export type EditableContentPackage = Pick<ContentPackage, 'name' | 'description'>;

/**
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ContentPackageBO<T extends ModelType = DefaultModelType> extends ContentPackage<T> {
    id: string;
}

export type StagableEntityType = 'file' | 'folder' | 'form' | 'image' | 'page';

export enum StagingErrorKind {
    EXCEPTION = 'EXCEPTION',
    MISMATCH = 'MISMATCH',
    DUPLICATION = 'DUPLICATION',
    NO_BINARY = 'NO_BINARY',
    OTHER = 'OTHER',
}

export interface StagedError {
    error: string;
    kind: StagingErrorKind;
}


export interface ContentPackageImportError {
    path: string;
    error: string;
    globalId: string;
    kind: string;
    recommendation: string;
}

export interface ContentPackageFolderObject {
    errors: StagedError[];
}

export interface StagingBase {
    /** Global/UUID of the object */
    id: string;
    /** Name of the object */
    name: string;
}

export interface StagedLocalizableObject extends StagingBase {
    /** Global/UUID of the master object, if obejct is localized */
    masterId?: string;
    /** Whether this object is excluded from multichannelling */
    excluded?: boolean;
    /** Whether this object is disinherited by default in new channels */
    disinheritDefault?: boolean;
    /** Global/UUIDs of the channels, from which the object is disinherited */
    disinheritedChannelIds?: string[];
}

export interface StagedReference {
    /** Global/UUID of the referenced object */
    id: string;
    /** Hash of the referenced object when the reference was created */
    hash: string;
}

export interface StagedNodeIdObjectId {
    nodeId: string;
    objectId: string;
}

export interface StagedOverview extends StagingBase, Pick<Overview,
| 'listType'
| 'selectType'
| 'orderDirection'
| 'orderBy'
| 'maxItems'
| 'recursive'
> {
    selectedNodeItemIds: StagedNodeIdObjectId[];
}

export interface StagedProperty extends
    StagingBase,
    Pick<TagPartProperty, 'type' | 'globalId' | 'partId'>,
    Partial<Pick<StringTagPartProperty, 'stringValue'>>,
    Partial<Pick<BooleanTagPartProperty, 'booleanValue'>>,
    Partial<Pick<FileTagPartProperty, 'fileId'>>,
    Partial<Pick<ImageTagPartProperty, 'imageId'>>,
    Partial<Pick<FolderTagPartProperty, 'folderId'>>,
    Partial<Pick<PageTagPartProperty, 'pageId'>>,
    Partial<Pick<TemplateTagTagPartProperty, 'templateId' | 'templateTagId'>>,
    Partial<Pick<PageTagTagPartProperty, 'contentTagId'>>,
    Partial<Pick<NodeTagPartProperty, 'nodeId'>>,
    Partial<Pick<FormTagPartProperty, 'formId'>>,
    Partial<Pick<ListTagPartProperty, 'stringValues'>>,
    Partial<Pick<SelectTagPartProperty, 'options' | 'selectedOptions'>>
{
    datasource?: StagedReference;
    overview?: StagedOverview;
    hash: string;
}

export interface StagedTag extends StagingBase, Pick<Tag,
| 'active'
| 'type'
> {
    construct: StagedReference;
    properties: Record<string, StagedProperty>;
}

export interface StagedObjectTag extends StagedTag {
    definition: StagedReference;
}

export enum StagedObjectVersionType {
    PUBLISHED = 'published',
    SCHEDULED = 'scheduled',
    CURRENT = 'current'
}

export interface StagedObjectVersion {
    versionNumber: string;
    type: StagedObjectVersionType;
    timestamp: number;
    publishAt: number;
    online?: boolean;
    timeOff?: number;
}

export interface StagedNode extends StagingBase, Pick<Node,
| 'publishDir'
| 'binaryPublishDir'
| 'pubDirSegment'
| 'publishImageVariants'
| 'publishFs'
| 'publishFsPages'
| 'publishFsFiles'
| 'publishContentMap'
| 'publishContentMapPages'
| 'publishContentMapFiles'
| 'publishContentMapFolders'
| 'omitPageExtension'
| 'pageLanguageCode'
| 'meshPreviewUrlProperty'
> {
    // Use global IDs instead of local ones
    /** Global/UUID of the content-repository */
    contentRepositoryId?: string;

    // Renamed properties
    /** Hostname of the node */
    hostname: Node['host'];
    /** Hostname property of the node */
    hostnameProperty: Node['hostProperty'];
    /** How URLs are rendered for pages in this node */
    pageUrls: Node['urlRenderWayPages'];
    /** How URLs are rendered for files in this node */
    fileUrls: Node['urlRenderWayFiles'];

    /** Global/UUID of the root-folder to import/use */
    rootFolderID: string;
    // Extra properties
    /** The languages which are assigned to this node */
    languages: string[];
    /** The features which are assigned to this node */
    features: NodeFeature[];
    /** The construct global-ids/uuids which are assigned to this node */
    constructIds: string[];
    /** The object-property global-ids/uuids which are restricted to this node */
    objectPropertyIds: string[];
    /** The devtool-packages which are assigned to this node */
    packages: string[];
}

export type ContentPackageNode = ContentPackageFolderObject & StagedNode;

export interface StagedChannel extends StagedNode {
    /** Global/UUID of the (direct) master node */
    masterNodeId: string;
}

export type ContentPackageChannel = ContentPackageFolderObject & StagedChannel;

export interface StagedFolder extends StagedLocalizableObject, Pick<Folder,
| 'publishDir'
| 'description'
| 'nameI18n'
| 'descriptionI18n'
| 'publishDirI18n'
> {
    objectTags: Record<string, StagedObjectTag>;
    templateIds: string[];
}

export type ContentPackageFolder = ContentPackageFolderObject & StagedFolder;

export interface StagedFile extends StagedLocalizableObject, Pick<File,
| 'fileType'
| 'description'
| 'fileSize'
| 'niceUrl'
| 'alternateUrls'
| 'forceOnline'
| 'customCdate'
| 'customEdate'
> {
    objectTags: Record<string, StagedObjectTag>;
}

export type ContentPackageFile = ContentPackageFolderObject & StagedFile;

export interface StagedImage extends StagedFile, Pick<Image,
| 'sizeX'
| 'sizeY'
| 'dpiX'
| 'dpiY'
| 'fpX'
| 'fpY'
| 'gisResizable'
> { }

export type ContentPackageImage = ContentPackageFolderObject & StagedImage;

export interface StagedPage extends StagedLocalizableObject, Pick<Page,
| 'niceUrl'
| 'alternateUrls'
| 'fileName'
| 'description'
| 'priority'
| 'language'
| 'contentId'
| 'customCdate'
| 'customEdate'
> {
    version: StagedObjectVersion;
    template: StagedReference;
    contentsetId: Page['contentSetId'];
    tags: Record<string, StagedTag>;
    objectTags: Record<string, StagedObjectTag>;
}

export type ContentPackagePage = ContentPackageFolderObject & StagedPage;

export interface StagedForm extends StagingBase, Pick<Form,
| 'description'
| 'languages'
| 'data'
> {
    version: StagedObjectVersion;
    successPageId: string;
    successNodeId: string;
}

export type ContentPackageForm = ContentPackageFolderObject & StagedForm;
