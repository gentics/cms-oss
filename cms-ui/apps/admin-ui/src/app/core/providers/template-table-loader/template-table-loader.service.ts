import {
    BO_DISPLAY_NAME,
    BO_ID, BO_PERMISSIONS,
    EntityPageResponse,
    PackageTableEntityLoader,
    TableLoadOptions,
    TemplateBO,
    applyPermissions,
} from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { PagedTemplateListResponse, Template, TemplateListRequest } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { TemplateOperations } from '../operations';

export interface TemplateTableLoaderOptions {
    nodeId?: number | number[];
    packageName?: string;
}

@Injectable()
export class TemplateTableLoaderService extends BaseTableLoaderService<Template, TemplateBO, TemplateTableLoaderOptions>
    implements PackageTableEntityLoader<TemplateBO, TemplateTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: TemplateOperations,
    ) {
        super('template', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: TemplateTableLoaderOptions,
    ): Observable<EntityPageResponse<TemplateBO>> {
        const loadOptions: TemplateListRequest = this.createDefaultOptions(options);
        let loader: Observable<PagedTemplateListResponse>;

        if (additionalOptions?.packageName) {
            loader = this.api.devTools.getTemplates(additionalOptions.packageName, loadOptions);
        } else {
            if (additionalOptions?.nodeId) {
                loadOptions.nodeId = additionalOptions.nodeId;
                loadOptions.perms = true;
            } else {
                loadOptions.reduce = true;
            }

            loader = this.api.template.getTemplates(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(template => this.mapToBusinessObject(template));
                applyPermissions(entities, response);

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    addToDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void> {
        return this.operations.addToDevTool(devToolPackage, entityId);
    }

    removeFromDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void> {
        return this.operations.removeFromDevTool(devToolPackage, entityId);
    }

    public mapToBusinessObject(template: Template): TemplateBO {
        return {
            ...template,
            [BO_ID]: String(template.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: template.name,
        };
    }
}
