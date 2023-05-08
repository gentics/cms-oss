import { ContentRepositoryOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Node, Normalized } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { BehaviorSubject } from 'rxjs';
import { delay } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-content-repositories-to-nodes-modal',
    templateUrl: './assign-content-repositories-to-nodes-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignContentrepositoriesToNodesModalComponent extends BaseModal<number[]> implements OnInit {

    /** ID of group to be (un)assigned to/from nodes */
    contentRepositoryId: string;

    /** IDs of nodes to be (un)assigned to/from contentRepository */
    nodeIdsInitial: number[];
    nodeIdsSelected: number[];

    /** Is TRUE if a logged-in user examines Node restrictions of a user whose restricted Nodes they're not have permission to read. */
    hiddenNodeIdsExist$ = new BehaviorSubject<boolean>(false);

    constructor(
        protected changeDetector: ChangeDetectorRef,
        private contentRepositoryOperations: ContentRepositoryOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        this.contentRepositoryOperations.getAssignedNodes(this.contentRepositoryId).pipe(
            delay(0),
        ).subscribe((node: Node<Normalized>[]) => {
            this.nodeIdsSelected = node.map(n => n.id);
            this.nodeIdsInitial = this.nodeIdsSelected.slice();
            this.changeDetector.markForCheck();
        });
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignContentRepositoryToNodesClicked(): void {
        this.contentRepositoryOperations
            .changeAssignedNodesOfContentRepository(this.contentRepositoryId, this.nodeIdsSelected)
            .toPromise()
            .then(() => this.closeFn(this.nodeIdsSelected));
    }
}
