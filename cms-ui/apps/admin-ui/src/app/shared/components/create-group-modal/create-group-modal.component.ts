import { GroupOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Group, GroupCreateRequest, Normalized, Raw } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { GroupDataService } from '../../providers/group-data/group-data.service';

@Component({
    selector: 'gtx-create-group-modal',
    templateUrl: './create-group-modal.component.html',
    styleUrls: ['./create-group-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateGroupModalComponent implements IModalDialog, OnInit {

    /** Current step (tab) of the entity creation wizzard */
    currentTab = String(1);

    /** form instance */
    form: UntypedFormGroup;

    /** Group id the new group will be created in. */
    parentGroupId: number;

    /** Group the new group will be created in. */
    parentGroup$: Observable<Group<Normalized>>;

    constructor(
        private groupData: GroupDataService,
        private groups: GroupOperations,
    ) {
    }

    ngOnInit(): void {
        this.parentGroup$ = this.groupData.getEntityFromState(this.parentGroupId) as Observable<Group<Normalized>>;

        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl(null),
            description: new UntypedFormControl(null),
        });
    }

    closeFn = (entityCreated: Group<Raw>) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: Group<Raw>) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /** Get form validity state */
    isValid(): boolean {
        return this.form.valid && Number.isInteger(this.parentGroupId);
    }

    /** Programmatic tab set */
    setActiveTab(index: string): void {
        this.currentTab = String(index);
    }

    /**
     * Returns TRUE if parameter index is index of active tab
     */
    tabIndexIsActive(index: number): boolean {
        return this.currentTab === String(index);
    }

    /**
     * If group clicks to create a new group
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(groupCreated => this.closeFn(groupCreated));
    }

    private createEntity(): Promise<Group<Raw>> {
        // assemble payload with conditional properties
        const group: GroupCreateRequest = {
            name: this.form.value.name,
            ...(this.form.value.description && { description: this.form.value.description }),
        };
        return this.groups.createSubgroup(this.parentGroupId, group).toPromise();
    }

}
