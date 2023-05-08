import { PackageOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { first, map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-packages-to-node-modal',
    templateUrl: './assign-packages-to-node-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignPackagesToNodeModalComponent extends BaseModal<boolean> implements IModalDialog, OnInit {

    /** ID of node to be (un)assigned to/from users */
    nodeId: number;

    /** IDs of users to be (un)assigned to/from nodes */
    packageIds$: Observable<string[]>;
    packageIdsInitial: string[] = [];
    packageIdsCurrent: string[] = [];

    constructor(
        private entityOperations: PackageOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        this.packageIds$ = this.entityOperations.getPackagesOfNode(this.nodeId).pipe(
            map(entities => entities.map(entity => entity.id)),
        );
        this.packageIds$.pipe(
            first(),
        ).subscribe(ids => {
            this.packageIdsInitial = ids;
            this.packageIdsCurrent = [...this.packageIdsInitial];
        });
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.packageIdsCurrent && this.packageIdsCurrent.length > 0;
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignPackagesToNodeClicked(): void {
        this.changePackagesOfNode()
            .then(didChange => this.closeFn(didChange));
    }

    private changePackagesOfNode(): Promise<boolean> {
        return this.entityOperations.changePackagesOfNode(
            this.nodeId,
            this.packageIdsCurrent,
            this.packageIdsInitial,
        ).toPromise();
    }

}
