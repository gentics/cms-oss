import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Schema } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshSchemaBO } from '../../common';
import { SchemaHandlerService } from '../schema-handler/schema-handler.service';

@Injectable()
export class SchemaTableLoaderService extends BaseTableLoaderService<Schema, MeshSchemaBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: SchemaHandlerService,
    ) {
        super(
            null,
            entityManager,
            appState,
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: never): Observable<EntityPageResponse<MeshSchemaBO>> {
        return from(this.handler.listMapped({
            page: options.page + 1,
            perPage: options.perPage,
            order: options.sortOrder?.toLowerCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map((res) => ({
                entities: res.data,
                // eslint-disable-next-line no-underscore-dangle
                totalCount: res._metainfo.totalCount,
            })),
        );
    }
}
