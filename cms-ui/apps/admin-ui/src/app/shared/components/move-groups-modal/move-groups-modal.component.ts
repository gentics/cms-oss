import { GroupBO } from '@admin-ui/common';
import { EntityManagerService, GroupOperations, I18nService } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Group, Normalized, Raw } from '@gentics/cms-models';
import { BaseModal, TableRow } from '@gentics/ui-core';
import { forkJoin, Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';

@Component({
    selector: 'gtx-move-groups-modal',
    templateUrl: './move-groups-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MoveGroupsModalComponent extends BaseModal<Group<Raw>[] | boolean> {

    /** IDs of groups to be moved */
    sourceGroupIds: (string | number)[] = [];

    /** IDs of groups the source groups shall be moved to */
    targetGroupId: number;

    constructor(
        private entityManager: EntityManagerService,
        private groupOperations: GroupOperations,
        private i18n: I18nService,
    ) {
        super();
    }

    handleRowClick(row: TableRow<GroupBO>): void {
        this.targetGroupId = row.id ? Number(row.id) : null;
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.sourceGroupIds && this.sourceGroupIds.length > 0 &&
            Number.isInteger(this.targetGroupId);
    }

    /**
     * If user clicks "assign"
     */
    buttonMoveClicked(): void {
        this.moveEntities()
            .then(movedGroups => this.closeFn(movedGroups));
    }

    getModalTitle(): Observable<string> {
        if (this.sourceGroupIds.length === 1) {
            return this.entityManager.getEntity('group', this.sourceGroupIds[0]).pipe(
                mergeMap((group: Group<Normalized>) => this.i18n.get('shared.move_group_to_groups_title', { entityName: group.name })),
            );
        } else {
            return this.i18n.get('shared.move_groups_to_groups_title');
        }
    }

    private moveEntities(): Promise<Group<Raw>[]> {
        return forkJoin(this.sourceGroupIds.map(sourceGroupId => {
            return this.groupOperations.moveSubgroup(sourceGroupId, this.targetGroupId);
        })).toPromise();
    }

}
