import { Injectable } from '@angular/core';
import {
    AnyModelType,
    ContentRepository,
    File,
    Folder,
    Form,
    GcmsNormalizer,
    Group,
    Image,
    Language,
    Message,
    Node,
    Normalized,
    Page,
    Raw,
    Template,
    User,
} from '@gentics/cms-models';
import { EntityState, EntityTypesMap } from '../../../common/models';
import { ApplicationStateService } from '../../../state/providers/application-state/application-state.service';

/**
 * The EntityResolver is used to get an entity from the app state "entities" branch
 * based on that entity's id.
 */
@Injectable()
export class EntityResolver {

    entities: EntityState;

    protected normalizer = new GcmsNormalizer();

    constructor(private appState: ApplicationStateService) {
        appState.select(state => state.entities).subscribe(entities => {
            this.entities = entities;
        });
    }

    getContentRepository(id: number): ContentRepository<Normalized> {
        return this.getEntity('contentRepository', id);
    }

    getFile(id: number): File<Normalized> {
        return this.getEntity('file', id);
    }

    getForm(id: number): Form<Normalized> {
        return this.getEntity('form', id);
    }

    getFolder(id: number): Folder<Normalized> {
        return this.getEntity('folder', id);
    }

    getGroup(id: number): Group<Normalized> {
        return this.getEntity('group', id);
    }

    getImage(id: number): Image<Normalized> {
        return this.getEntity('image', id);
    }

    getMessage(id: number): Message<Normalized> {
        return this.getEntity('message', id);
    }

    getNode(id: number): Node<Normalized> {
        return this.getEntity('node', id);
    }

    getPage(id: number): Page<Normalized> {
        return this.getEntity('page', id);
    }

    getTemplate(id: number): Template<Normalized> {
        return this.getEntity('template', id);
    }

    getUser(id: number): User<Normalized> {
        return this.getEntity('user', id);
    }

    getLanguage(id: number): Language {
        return this.getEntity('language', id);
    }

    getEntity<T extends keyof EntityState, R extends EntityTypesMap<Normalized>[T]>(entityType: T, id: number): R {
        const branch = this.entities[entityType];
        return branch && branch[id] as unknown as R;
    }

    /**
     * Given a template name, returns the corresponding Template entity.
     */
    getTemplateByName(name: string): Template<Normalized> {
        const templates = this.entities.template;
        return this.find(templates, 'name', name) || {};
    }

    /**
     * Given a language name, returns the corresponding Language entity.
     */
    getLanguageByName(name: string): Language {
        const languages = this.entities.language;
        return this.find(languages, 'name', name) || {};
    }

    /**
     * Given a language code ('en', 'de' etc.) returns the corresponding
     * Language entity.
     */
    getLanguageByCode(code: string): Language {
        const languages = this.entities.language;
        return this.find(languages, 'code', code) || {};
    }

    /**
     * Denormalizes an entity using the entities available in the AppState.
     *
     * @param type The type of entity.
     * @param entity The normalized entity that should be denormalized.
     * For ease of use, also a denormalized or partially denormalized entity may be passed here.
     * @returns The denormalized version of the entity.
     */
    denormalizeEntity<T extends keyof EntityState, E extends EntityTypesMap<AnyModelType>[T], R extends EntityTypesMap<Raw>[T]>(
        type: T,
        entity: E | null | undefined,
    ): R {
        return this.normalizer.denormalize(type as any, entity, this.entities);
    }

    /**
     * Given a hash of objects, returns the first value where the key matches the value val.
     */
    private find<T, K extends keyof T>(hash: { [id: number]: T }, key: K, val: T[K]): any {
        for (let k in hash) {
            if (hash[k][key] === val) {
                return hash[k];
            }
        }
    }

}
