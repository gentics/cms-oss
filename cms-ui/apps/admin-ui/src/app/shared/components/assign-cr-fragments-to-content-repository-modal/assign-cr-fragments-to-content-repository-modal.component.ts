import { ContentRepositoryOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ContentRepositoryFragmentBO, Normalized } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { delay } from 'rxjs/operators';

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
        private contentRepositoryOperations: ContentRepositoryOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        this.contentRepositoryOperations.getAssignedFragments(this.contentRepositoryId).pipe(
            delay(0),
        ).subscribe((crfragment: ContentRepositoryFragmentBO<Normalized>[]) => {
            this.crfragmentIdsSelected = crfragment.map(n => String(n.id));
            this.crfragmentIdsInitial = this.crfragmentIdsSelected.slice();
            this.changeDetector.markForCheck();
        });
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignContentRepositoryToCrfragmentsClicked(): void {
        this.contentRepositoryOperations
            .changeFragmentsOfContentRepository(this.contentRepositoryId, this.crfragmentIdsSelected)
            .toPromise()
            .then(() => this.closeFn(this.crfragmentIdsSelected));
    }
}
