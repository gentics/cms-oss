import { Form } from '../cms-form';
import { ConstructCategory, ConstructCategoryBO } from '../construct-category';
import { ContentPackage, ContentPackageBO } from '../content-package';
import { ContentRepository, ContentRepositoryBO } from '../content-repository';
import { ContentRepositoryFragment, ContentRepositoryFragmentBO } from '../cr-fragment';
import { DataSource, DataSourceBO } from '../data-source';
import { DataSourceEntry, DataSourceEntryBO } from '../data-source-entry';
import { ElasticSearchIndex } from '../elastic-search-index';
import { File } from '../file';
import { Folder } from '../folder';
import { Group } from '../group';
import { Image } from '../image';
import { Language } from '../language';
import { ActionLogEntry } from '../logs';
import { MarkupLanguage } from '../markup-language';
import { Message } from '../message';
import { Node } from '../node';
import { ObjectProperty, ObjectPropertyBO } from '../object-property';
import { ObjectPropertyCategory, ObjectPropertyCategoryBO } from '../object-property-category';
import { Package, PackageBO } from '../package';
import { Page } from '../page';
import { Role, RoleBO } from '../role';
import { Schedule, ScheduleBO } from '../schedule';
import { ScheduleExecution } from '../schedule-execution';
import { ScheduleTask, ScheduleTaskBO } from '../schedule-task';
import { TagStatus, TagStatusBO, TagType, TagTypeBO, TemplateTag } from '../tag';
import { TagmapEntry, TagmapEntryBO } from '../tagmap-entry';
import { Template, TemplateBO } from '../template';
import { DefaultModelType, IndexById, ModelType, Normalized } from '../type-util';
import { User } from '../user';

/** Maps the names of normalizable model types to their interfaces. */
export interface NormalizableEntityTypesMap<T extends ModelType = DefaultModelType> {
    construct: TagType<T>;
    constructCategory: ConstructCategory<T>;
    contentPackage: ContentPackage<T>;
    contentRepository: ContentRepository<T>;
    contentRepositoryFragment: ContentRepositoryFragment<T>;
    dataSource: DataSource<T>;
    dataSourceEntry: DataSourceEntry<T>;
    elasticSearchIndex: ElasticSearchIndex<T>;
    file: File<T>;
    folder: Folder<T>;
    form: Form<T>;
    group: Group<T>;
    image: Image<T>;
    language: Language;
    logs: ActionLogEntry;
    markupLanguage: MarkupLanguage<T>;
    message: Message<T>;
    node: Node<T>;
    objectProperty: ObjectProperty<T>;
    objectPropertyCategory: ObjectPropertyCategory<T>;
    package: Package<T>;
    page: Page<T>;
    role: Role<T>;
    schedule: Schedule<T>;
    scheduleExecution: ScheduleExecution<T>;
    scheduleTask: ScheduleTask<T>;
    tagmapEntry: TagmapEntry<T>;
    template: Template<T>;
    templateTag: TemplateTag;
    templateTagStatus: TagStatus;
    user: User<T>;
}

/**
 * Maps the names of normalizable model types to their interfaces.
 * This Business Object version is defined for substituted `id` properties mapped by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface NormalizableEntityTypesMapBO<T extends ModelType = DefaultModelType> {
    construct: TagTypeBO<T>;
    constructCategory: ConstructCategoryBO<T>;
    contentPackage: ContentPackageBO<T>;
    contentRepository: ContentRepositoryBO<T>;
    contentRepositoryFragment: ContentRepositoryFragmentBO<T>;
    dataSource: DataSourceBO<T>;
    dataSourceEntry: DataSourceEntryBO<T>;
    elasticSearchIndex: ElasticSearchIndex<T>;
    file: File<T>;
    folder: Folder<T>;
    form: Form<T>;
    group: Group<T>;
    image: Image<T>;
    language: Language;
    logs: ActionLogEntry;
    markupLanguage: MarkupLanguage<T>;
    message: Message<T>;
    node: Node<T>;
    objectProperty: ObjectPropertyBO<T>;
    objectPropertyCategory: ObjectPropertyCategoryBO<T>;
    package: PackageBO<T>;
    page: Page<T>;
    role: RoleBO<T>;
    schedule: ScheduleBO<T>;
    scheduleExecution: ScheduleExecution<T>;
    scheduleTask: ScheduleTaskBO<T>;
    tagmapEntry: TagmapEntryBO<T>;
    template: TemplateBO<T>;
    templateTag: TemplateTag;
    templateTagStatus: TagStatusBO;
    user: User<T>;
}

/** The names of normalizable model types, as they are used for the normalizr schemas. */
export type NormalizableEntityType = keyof NormalizableEntityTypesMap;

/**
 * Represents a store of normalized entities.
 *
 * The name of an entity model type maps to an `IndexById`, where the id is the ID
 * of the respective entity and the value is the normalized version of that entity.
 */
export type NormalizedEntityStore = {
    [K in NormalizableEntityType]: IndexById<NormalizableEntityTypesMap<Normalized>[K]>
};
/**
 * @deprecated Create your own application specific type/business object instead.
 */
export type NormalizedEntityStoreBO = {
    [K in NormalizableEntityType]: Record<EntityIdType, NormalizableEntityTypesMap<Normalized>[K]>
};

/**
 * The result of a normalization operation.
 */
export interface NormalizationResult {
    /**
     * The normalized entities.
     *
     * Note that only those entity store branches are set, which actually contain entities.
     */
    entities: Partial<NormalizedEntityStore>;
}

/**
 * The result of normalizing an array.
 */
export interface ArrayNormalizationResult extends NormalizationResult { }

/**
 * The result of normalizing a single entity.
 *
 * - `resultId` contains the ID of the normalized entity.
 * - `result` contains the normalized entity
 *  (the same as e.g. for a page `normalizationResult.entities.page[normalizationResult.resultId]`)
 */
export interface SingleNormalizationResult<T> extends NormalizationResult {

    /** The ID of the normalized entity. */
    resultId: number;

    /** The normalized entity. */
    result: T;

}

/**
 * Normalizable entity's property `id` types.
 */
export type EntityIdType = NormalizableEntityTypesMapBO<Normalized>[NormalizableEntityType]['id'];
