import { ElasticSearchIndexBO } from '@admin-ui/common';
import { ElasticSearchIndexOperations, I18nService, PermissionsService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, ElasticSearchIndex, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ElasticSearchIndexTableLoaderService } from '../../providers';

const REBUILD_ACTION = 'rebuildIndex';
const DELETE_AND_REBUILD_ACTION = 'deleteAndRebuildIndex';

@Component({
    selector: 'gtx-elastic-search-index-table',
    templateUrl: './elastic-search-index-table.component.html',
    styleUrls: ['./elastic-search-index-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ElasticSearchIndexTableComponent extends BaseEntityTableComponent<ElasticSearchIndex, ElasticSearchIndexBO> {

    protected rawColumns: TableColumn<ElasticSearchIndexBO>[] = [
        {
            id: 'name',
            label: 'elasticSearchIndex.index_name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'found',
            label: 'elasticSearchIndex.index_found',
            fieldPath: 'found',
            align: 'center',
            sortable: true,
        },
        {
            id: 'settingsValid',
            label: 'elasticSearchIndex.index_settingsValid',
            fieldPath: 'settingsValid',
            align: 'center',
            sortable: true,
        },
        {
            id: 'mappingValid',
            label: 'elasticSearchIndex.index_mappingValid',
            fieldPath: 'mappingValid',
            align: 'center',
            sortable: true,
        },
        {
            id: 'indexed',
            label: 'elasticSearchIndex.index_indexed',
            fieldPath: 'indexed',
            align: 'right',
            sortable: true,
        },
        {
            id: 'objects',
            label: 'elasticSearchIndex.index_objects',
            fieldPath: 'objects',
            align: 'right',
            sortable: true,
        },
        {
            id: 'queued',
            label: 'elasticSearchIndex.index_queued',
            fieldPath: 'queued',
            align: 'right',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'elasticSearchIndex';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ElasticSearchIndexTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
        protected operations: ElasticSearchIndexOperations,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<ElasticSearchIndexBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.setPermContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canRebuild]) => {
                const actions: TableAction<ElasticSearchIndexBO>[] = [
                    {
                        id: REBUILD_ACTION,
                        icon: 'build',
                        label: this.i18n.instant('elasticSearchIndex.index_rebuild'),
                        type: 'primary',
                        enabled: canRebuild,
                        single: true,
                        multiple: true,
                    },
                    {
                        id: DELETE_AND_REBUILD_ACTION,
                        icon: 'autorenew',
                        label: this.i18n.instant('elasticSearchIndex.index_delete_and_rebuild'),
                        type: 'alert',
                        enabled: canRebuild,
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public override handleAction(event: TableActionClickEvent<ElasticSearchIndexBO>): void {
        switch (event.actionId) {
            case REBUILD_ACTION:
                this.rebuildIndex(this.getAffectedEntityIds(event), false);
                return;

            case DELETE_AND_REBUILD_ACTION:
                this.rebuildIndex(this.getAffectedEntityIds(event), true);
                return;
        }

        super.handleAction(event);
    }

    async rebuildIndex(indexNames: string[], deleteIndex: boolean): Promise<void> {
        for (const name of indexNames) {
            await this.operations.rebuild(name, deleteIndex).toPromise();
        }
        this.reload();
    }
}
