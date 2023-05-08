import { NodeDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Node, NodeTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-node-url-part-fill',
    templateUrl: './node-url-part-fill.component.html',
    styleUrls: ['./node-url-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(NodeUrlPartFillComponent)],
})
export class NodeUrlPartFillComponent extends BaseFormElementComponent<NodeTagPartProperty> implements OnInit {

    public nodes$: Observable<Node[]>;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected nodeData: NodeDataService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.nodes$ = this.nodeData.watchAllEntities();
    }

    protected onValueChange(): void { }

    nodeSelected(nodeId: number | null): void {
        const newValue: NodeTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: TagPropertyType.NODE,
            nodeId,
        };

        this.triggerChange(newValue);
    }
}
