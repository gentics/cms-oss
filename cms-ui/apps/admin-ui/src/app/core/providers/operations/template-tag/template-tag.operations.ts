import { Injectable, Injector } from '@angular/core';
import { EntityIdType, Tag, TemplateSaveRequest, TemplateSaveResponse, TemplateTag } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class TemplateTagOperations extends ExtendedEntityOperationsBase<'templateTag'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
    ) {
        super(injector, 'templateTag');
    }

    getAll(options?: null, templateId?: string | number): Observable<TemplateTag[]> {
        if (templateId == null || (typeof templateId === 'string' && templateId.length === 0)) {
            throw new Error('Invalid Argument: Template ID has to be provided');
        }

        // return this.api.template.getTemplateTags(templateId).pipe(
        //     map(res => res.tags),
        //     this.catchAndRethrowError(),
        // );

        return this.api.template.getTemplate(templateId, { construct: true }).pipe(
            map(template => Object.values(template.template.templateTags)),
            map(tags => {
                return tags.map(tag => ({
                    ...tag,
                    id: tag.name,
                    mandatory: tag.mandatory ?? false,
                }) as any as TemplateTag);
            }),
            tap(tags => this.entityManager.addEntities(this.entityIdentifier, tags)),
            this.catchAndRethrowError(),
        );
    }

    get(entityId: EntityIdType, options?: any, parentId?: EntityIdType): Observable<TemplateTag> {
        return this.entityManager.getEntity(this.entityIdentifier, entityId);
    }

    update(templateId: string | number, body: TemplateSaveRequest): Observable<TemplateSaveResponse> {
        return this.api.template.updateTemplate(templateId, body).pipe(
            switchMap(res => {
                // Reload the tags when they have been edited
                return this.getAll(null, templateId).pipe(
                    map(() => res)
                );
            }),
        );
    }
}
