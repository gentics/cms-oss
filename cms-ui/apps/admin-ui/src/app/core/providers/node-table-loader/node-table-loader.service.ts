import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, NodeBO, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Node, NodeListRequestOptions, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { NodeOperations } from '../operations';

@Injectable()
export class NodeTableLoaderService extends BaseTableLoaderService<Node, NodeBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: NodeOperations,
    ) {
        super('node', entityManager, appState);
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public isChannel(entity: Node<Raw>): boolean {
        if (entity?.masterNodeId !== entity?.id) {
            return true;
        }
        return false;
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.removeNode(entityId).then(() => {});
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<NodeBO>> {
        const loadOptions: NodeListRequestOptions = this.createDefaultOptions(options);
        loadOptions.perms = true;

        return this.api.node.getNodes(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(node => this.mapToBusinessObject(node));
                this.applyPermissions(entities, response);

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(node: Node<Raw>): NodeBO {
        return {
            ...node,
            [BO_ID]: String(node.id),
            [BO_DISPLAY_NAME]: node.name,
            [BO_PERMISSIONS]: [],
        };
    }
}
