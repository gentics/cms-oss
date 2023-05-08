import { discard } from '@admin-ui/common/utils/rxjs-discard-operator/discard.opertator';
import { Injectable, Injector } from '@angular/core';
import { EntityIdType, TagStatus, TagStatusBO } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class TemplateTagStatusOperations extends ExtendedEntityOperationsBase<'templateTagStatus'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
    ) {
        super(injector, 'templateTagStatus');
    }

    getAll(options?: null, templateId?: string | number): Observable<TagStatusBO[]> {
        if (templateId == null || (typeof templateId === 'string' && templateId.length === 0)) {
            throw new Error('Invalid Argument: Template ID has to be provided');
        }

        return this.api.template.getTemplateTagStatus(templateId).pipe(
            map(res => res.items.map(item => this.mapToBusinessObject(item))),
            this.catchAndRethrowError(),
        );
    }

    get(entityId: EntityIdType, options?: any, parentId?: EntityIdType): Observable<TagStatusBO> {
        return of(null);
    }

    synchronizeTags(templateId: string | number, tagNames: string[], forceSync: boolean): Observable<void> {
        return this.api.template.updateTemplate(templateId, {
            forceSync,
            sync: tagNames,
            syncPages: true,
        }).pipe(
            discard(),
            this.catchAndRethrowError(),
        );
    }

    private mapToBusinessObject(tagStatus: TagStatus): TagStatusBO {
        return {
            ...tagStatus,
            id: tagStatus.name,
        };
    }
}
