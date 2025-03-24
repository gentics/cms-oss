import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions, TemplateTagBO } from '@admin-ui/common';
import { EntityManagerService, TemplateTagOperations } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { TemplateTag, TemplateTagsRequestOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface TemplateTagTableLoaderOptions {
    templateId: number | string;
}

@Injectable()
export class TemplateTagTableLoaderService extends BaseTableLoaderService<TemplateTag, TemplateTagBO, TemplateTagTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: TemplateTagOperations,
    ) {
        super('templateTag', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(tagId: string, additionalOptions?: TemplateTagTableLoaderOptions): Promise<void> {
        return this.operations.delete(additionalOptions.templateId, tagId).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: TemplateTagTableLoaderOptions,
    ): Observable<EntityPageResponse<TemplateTagBO>> {
        const loadOptions: TemplateTagsRequestOptions = {
            ...this.createDefaultOptions(options),
            embed: 'construct',
        };

        return this.api.template.getTemplateTags(additionalOptions?.templateId, loadOptions).pipe(
            map(response => {
                const entities = response.items
                    .filter(tag => tag.type === 'TEMPLATETAG')
                    .map(tag => this.mapToBusinessObject(tag as TemplateTag));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(tag: TemplateTag): TemplateTagBO {
        return {
            ...tag,
            [BO_ID]: tag.name,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: tag.name,
        };
    }
}
