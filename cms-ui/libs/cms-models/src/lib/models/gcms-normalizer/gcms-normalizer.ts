import { denormalize, normalize, Schema, schema as SchemaNamespace } from 'normalizr';
import { FileOrImage } from '../file';
import { Folder } from '../folder';
import { Group } from '../group';
import { Message } from '../message';
import { Node } from '../node';
import { Package } from '../package';
import { Page } from '../page';
import { Template } from '../template';
import { AnyModelType, IS_NORMALIZED, Normalized, Raw } from '../type-util';
import { User } from '../user';
import {
    ArrayNormalizationResult,
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    NormalizationResult,
    NormalizedEntityStore,
    SingleNormalizationResult,
} from './gcms-normalizer-types';
import { GcmsNormalizationSchemas } from './schemas';

/**
 * This is a wrapper class around the normalizr library's `normalize()`
 * and `denormalize()` functions.
 *
 * It should be used instead of the normalizr functions, because it adds
 * additional post-processing.
 * @deprecated Normalization of models should be done on a application level based on the business logic.
 */
export class GcmsNormalizer {

    private static entitySchemas = new GcmsNormalizationSchemas();

    /**
     * @returns The normalizr schema for the specified entity type or undefined, if entityType is invalid.
     */
    getSchema(entityType: NormalizableEntityType): Schema {
        return GcmsNormalizer.entitySchemas[entityType];
    }

    /**
     * Normalizes a single entity.
     * @param type The name of the type of entity to be normalized.
     * @param entity The entity to be normalized.
     */
    normalize<
        T extends keyof NormalizableEntityTypesMapBO,
        E extends NormalizableEntityTypesMapBO<Raw>[T],
        R extends NormalizableEntityTypesMapBO<Normalized>[T],
    >(type: T, entity: E): SingleNormalizationResult<R>;
    /**
     * Normalizes an array of entities.
     * @param type The name of the type of the entities in the array.
     * @param entities The array of entities to be normalized.
     */
    normalize<
        T extends keyof NormalizableEntityTypesMapBO,
        E extends NormalizableEntityTypesMapBO<Raw>[T],
    >(type: T, entities: E[]): ArrayNormalizationResult;
    normalize<
        T extends keyof NormalizableEntityTypesMapBO,
        E extends NormalizableEntityTypesMapBO<Raw>[T],
        R extends NormalizableEntityTypesMapBO<Normalized>[T],
    >(type: T, entityOrEntities: E | E[]): NormalizationResult {
        let schema = this.getSchema(type);
        if (!schema) {
            throw new Error(`No Schema found for entity type ${type}.`);
        }
        const isArray = Array.isArray(entityOrEntities);
        if (isArray) {
            schema = new SchemaNamespace.Array(schema);
        }
        const normalizrResult = normalize(entityOrEntities, schema);

        const result: NormalizationResult = {
            entities: normalizrResult.entities,
        };
        if (!isArray) {
            (result as SingleNormalizationResult<R>).resultId = normalizrResult.result;
            (result as SingleNormalizationResult<R>).result = normalizrResult.entities[type][normalizrResult.result];
        }
        return result;
    }

    /**
     * Denormalizes an entity using the entities available in the specified entitiesStore.
     * @param type The type of entity.
     * @param entity The normalized entity that should be denormalized.
     * For ease of use, also a denormalized or partially denormalized entity may be passed here.
     * @param entityStore The objects that contains all normalized entities.
     * @returns The denormalized version of the entity.
     */
    denormalize<
        T extends keyof NormalizableEntityTypesMapBO,
        E extends NormalizableEntityTypesMapBO<AnyModelType>[T],
        R extends NormalizableEntityTypesMapBO<Raw>[T],
    >(
        type: T,
        entity: E | null | undefined,
        entityStore: Partial<NormalizedEntityStore>,
    ): R {
        if (entity) {
            const schema = this.getSchema(type);
            let denormalizedEntity: R;
            if (schema && entity[IS_NORMALIZED]) {
                denormalizedEntity = denormalize(entity, schema, entityStore);
            } else {
                denormalizedEntity = { ...entity } as any;
            }
            this.postProcessDenormalizedEntity(type, denormalizedEntity);
            return denormalizedEntity;
        }
        return null;
    }

    /**
     * Removes properties from the objects nested in the denormalized objects, which were not denormalized,
     * because of redundancy or because of the possibility for an infinite loop.
     */
    private postProcessDenormalizedEntity<T extends keyof NormalizableEntityTypesMapBO, E extends NormalizableEntityTypesMapBO<Raw>[T]>(
        type: T,
        entity: E,
    ): void {
        delete entity[IS_NORMALIZED];
        switch (type) {
            case 'page':
                this.processDenormalizedPage(entity as Page<Raw>);
                break;
            case 'form':
                break;
            case 'folder':
                this.processDenormalizedFolder(entity as Folder<Raw>);
                break;
            case 'file':
            case 'image':
                this.processDenormalizedFileOrImage(entity as FileOrImage<Raw>);
                break;
            case 'node':
                this.processDenormalizedNode(entity as Node<Raw>);
                break;
            case 'template':
                this.processDenormalizedTemplate(entity as unknown as Template<Raw>);
                break;
            case 'user':
                this.processDenormalizedUser(entity as User<Raw>);
                break;
            case 'group':
                this.processDenormalizedGroup(entity as Group<Raw>);
                break;
            case 'message':
                this.processDenormalizedMessage(entity as Message<Raw>);
                break;
            case 'package':
                this.processDenormalizedPackage(entity as Package<Raw>);
                break;
            default:
                break;
        }
    }

    private processDenormalizedFileOrImage(fileOrImage: FileOrImage<Raw>): void {
        this.processDenormalizedUsers(fileOrImage);
        if (fileOrImage.folder) {
            this.processDenormalizedFolder(fileOrImage.folder);
        }
    }

    private processDenormalizedFolder(folder: Folder<Raw>, isNested: boolean = false): void {
        this.processDenormalizedUsers(folder);
        if (folder.subfolders) {
            if (isNested) {
                delete folder.subfolders;
            } else {
                folder.subfolders.forEach((subfolder) => this.processDenormalizedFolder(subfolder, true));
            }
        }
    }

    private processDenormalizedGroup(group: Group<Raw>): void {
        if (group.children) {
            group.children.forEach((subGroup) => subGroup.children && delete subGroup.children);
        }
    }

    private processDenormalizedMessage(message: Message<Raw>): void {
        if (message.sender) {
            this.processDenormalizedUser(message.sender);
        }
    }

    private processDenormalizedNode(node: Node<Raw>): void {
        this.processDenormalizedUsers(node);
    }

    private processDenormalizedPage(page: Page<Raw>, isNested: boolean = false): void {
        this.processDenormalizedUsers(page);
        if (page.publisher) {
            this.processDenormalizedUser(page.publisher);
        }
        if (page.folder) {
            this.processDenormalizedFolder(page.folder);
        }
        if (page.languageVariants) {
            if (isNested) {
                delete (page as any).languageVariants;
            } else {
                for (const key of Object.keys(page.languageVariants)) {
                    this.processDenormalizedPage(page.languageVariants[key], true);
                }
            }
        }
        if (page.pageVariants) {
            if (isNested) {
                delete (page as any).pageVariants;
            } else {
                for (const key of Object.keys(page.pageVariants)) {
                    this.processDenormalizedPage(page.pageVariants[key], true);
                }
            }
        }
    }

    private processDenormalizedTemplate(template: Template<Raw>): void {
        this.processDenormalizedUsers(template);
    }

    private processDenormalizedUser(user: User<Raw>): void {
        if (user.groups) {
            user.groups.forEach((group) => this.processDenormalizedGroup(group));
        }
    }

    private processDenormalizedUsers(entity: { creator?: User<Raw>; editor?: User<Raw> }): void {
        if (entity.creator) {
            this.processDenormalizedUser(entity.creator);
        }
        if (entity.editor) {
            this.processDenormalizedUser(entity.editor);
        }
    }

    private processDenormalizedPackage(cmsPackage: Package<Raw>): void {
        if (cmsPackage.subPackages) {
            cmsPackage.subPackages.forEach((p) => this.processDenormalizedPackage(p));
        }
    }

}
