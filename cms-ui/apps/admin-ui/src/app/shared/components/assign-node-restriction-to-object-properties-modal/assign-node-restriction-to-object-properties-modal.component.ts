import { ObjectPropertyBO } from '@admin-ui/common';
import { ObjectPropertyOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { EntityIdType, Node, Raw } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { BehaviorSubject } from 'rxjs';
import { delay } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-node-restriction-to-object-properties-modal',
    templateUrl: './assign-node-restriction-to-object-properties-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignNodeRestrictionsToObjectPropertiesModalComponent extends BaseModal<void> implements OnInit {

    /** IDs of objectproperties to be (un)assigned to/from groups */
    objectProperty: ObjectPropertyBO;

    /** IDs of nodes to be (un)assigned to/from groups */
    nodeIdsInitial: EntityIdType[];
    nodeIdsSelected: EntityIdType[];

    /** Is TRUE if a logged-in user examines Node restrictions of a user whose restricted Nodes they're not have permission to read. */
    hiddenNodeIdsExist$ = new BehaviorSubject<boolean>(false);

    constructor(
        protected changeDetector: ChangeDetectorRef,
        private objectPropertyOperations: ObjectPropertyOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        this.objectPropertyOperations.getLinkedNodes(this.objectProperty.globalId).pipe(
            delay(0),
        ).subscribe((res: Node<Raw>[]) => {
            this.nodeIdsSelected = res.map(n => n.id);
            this.nodeIdsInitial = res.map(n => n.id);
            this.changeDetector.markForCheck();
        });
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignNodeRestrictonsClicked(): void {
        this.objectPropertyOperations.changeNodeRestrictions(this.objectProperty.globalId, this.nodeIdsSelected, this.nodeIdsInitial)
            .toPromise()
            .then(() => {
                this.closeFn();
            });
    }
}
