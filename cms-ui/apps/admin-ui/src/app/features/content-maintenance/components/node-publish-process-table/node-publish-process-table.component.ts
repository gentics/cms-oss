import { NodeBO } from '@admin-ui/common';
import { I18nService, NodeTableLoaderService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { AnyModelType, Node, NormalizableEntityTypesMap, PublishQueue } from '@gentics/cms-models';
import { ModalService, TableColumn } from '@gentics/ui-core';
import { PUBLISH_PLURAL_MAPPING } from '../../models';

@Component({
    selector: 'gtx-node-publish-process-table',
    templateUrl: './node-publish-process-table.component.html',
    styleUrls: ['./node-publish-process-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodePublishProcessTableComponent extends BaseEntityTableComponent<Node, NodeBO> {

    public readonly PUBLISH_PLURAL_MAPPING = PUBLISH_PLURAL_MAPPING;

    @Input()
    public publishQueue: PublishQueue;

    protected rawColumns: TableColumn<NodeBO>[] = [
        {
            id: 'name',
            label: 'node.node_name',
            fieldPath: 'name',
            sortable: true,
        },
    ];

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'node';

    public expandedNodes = new Set<string>();

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: NodeTableLoaderService,
        modalService: ModalService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        )
    }
}
