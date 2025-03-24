import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions, TagStatusBO } from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { TagStatus, TagStatusOptions } from '@gentics/cms-models';
import { GcmsApi, stringifyPagingSortOptions } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface TemplateTagStatusTableLoaderOptions {
    templateId: number | string;
}

@Injectable()
export class TemplateTagStatusTableLoaderService extends BaseTableLoaderService<TagStatus, TagStatusBO, TemplateTagStatusTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
    ) {
        super(null, entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return Promise.reject(new Error('Deletion of tag-status is not supported/available!'));
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: TemplateTagStatusTableLoaderOptions,
    ): Observable<EntityPageResponse<TagStatusBO>> {
        const tmpOptions = this.createDefaultOptions(options);
        const loadOptions: TagStatusOptions = {
            page: tmpOptions.page,
            pageSize: tmpOptions.pageSize,
        };

        if (tmpOptions.q) {
            loadOptions.q = tmpOptions.q;
        }

        if (tmpOptions.sort) {
            loadOptions.sort = stringifyPagingSortOptions(tmpOptions.sort);
        }

        return this.api.template.getTemplateTagStatus(additionalOptions.templateId, loadOptions).pipe(
            map(response => {
                const entities = response.items.map(status => this.mapToBusinessObject(status));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(status: TagStatus): TagStatusBO {
        return {
            ...status,
            [BO_ID]: status.name,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: status.name,
        };
    }
}
