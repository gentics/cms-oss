import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { delay } from 'rxjs/operators';
import { ContentRepositoryHandlerService } from '../../providers/content-repository-handler/content-repository-handler.service';

@Component({
    selector: 'gtx-assign-cr-fragments-to-content-repository-modal',
    templateUrl: './assign-cr-fragments-to-content-repository-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignCRFragmentsToContentRepositoryModal extends BaseModal<string[]> implements OnInit {

    /** IDs of crfragments to be (un)assigned to/from contentRepository */
    crfragmentIds: string[];

    /** ID of group to be (un)assigned to/from crfragments */
    contentRepositoryId: string;

    /** IDs of crfragments to be (un)assigned to/from contentRepository */
    crfragmentIdsInitial: string[];
    crfragmentIdsSelected: string[];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        private handler: ContentRepositoryHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.handler.getAssignedFragments(this.contentRepositoryId).pipe(
            delay(0),
        ).subscribe(crFragments => {
            this.crfragmentIdsSelected = crFragments.map(n => String(n.id));
            this.crfragmentIdsInitial = this.crfragmentIdsSelected.slice();
            this.changeDetector.markForCheck();
        });
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignContentRepositoryToCrfragmentsClicked(): void {
        this.handler
            .changeFragmentsOfContentRepository(this.contentRepositoryId, this.crfragmentIdsSelected)
            .toPromise()
            .then(() => this.closeFn(this.crfragmentIdsSelected));
    }
}
