import { BO_ID } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DevToolPackageHandlerService } from '../../providers/dev-tool-package-handler/dev-tool-package-handler.service';
import { DevToolPackageManagerService } from '../../providers/dev-tool-package-manager/dev-tool-package-manager.service';

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
        private handler: DevToolPackageHandlerService,
        private manager: DevToolPackageManagerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.packageIds$ = this.handler.listFromNodeMapped(this.nodeId).pipe(
            map(pkgs => pkgs.items.map(entity => entity[BO_ID])),
        );
        this.packageIds$.subscribe(ids => {
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
        return this.manager.manageNodeAssignment(
            this.nodeId,
            this.packageIdsInitial,
            this.packageIdsCurrent,
        ).toPromise();
    }

}
