import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Group } from '@gentics/mesh-models';
import { Observable, forkJoin, from, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { MeshGroupBO } from '../../common';
import { MeshGroupHandlerService } from '../mesh-group-handler/mesh-group-handler.service';

export interface MeshGroupTableLoaderOptions {
    users?: boolean;
}

@Injectable()
export class MeshGroupTableLoaderService extends BaseTableLoaderService<Group, MeshGroupBO, MeshGroupTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: MeshGroupHandlerService,
    ) {
        super(
            null,
            entityManager,
            appState,
        );
    }

    public canDelete(): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: MeshGroupTableLoaderOptions): Observable<EntityPageResponse<MeshGroupBO>> {
        return from(this.handler.listMapped({
            page: Math.max(options.page, 1),
            perPage: options.perPage,
            order: options.sortOrder?.toLowerCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map((res) => ({
                entities: res.data,
                // eslint-disable-next-line no-underscore-dangle
                totalCount: res._metainfo.totalCount,
            })),
            switchMap(page => {
                if (!(additionalOptions?.users ?? false)) {
                    return of(page);
                }

                return forkJoin(page.entities.map(group => {
                    return from(this.handler.getUsers(group.uuid).then(userRes => {
                        group.users = userRes.data;
                        return group;
                    }));
                })).pipe(
                    map(mapped => {
                        return {
                            ...page,
                            entities: mapped,
                        };
                    }),
                );
            }),
        );
    }
}
