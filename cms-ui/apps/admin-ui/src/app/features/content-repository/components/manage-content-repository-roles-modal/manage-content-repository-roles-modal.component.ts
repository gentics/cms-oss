import { ContentRepositoryBO } from '@admin-ui/common';
import { ContentRepositoryHandlerService, I18nService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { BaseModal, TableColumn, TableRow, TableSelectAllType } from '@gentics/ui-core';
import { Subscription, combineLatest } from 'rxjs';

@Component({
    selector: 'gtx-manage-content-repository-roles-modal',
    templateUrl: './manage-content-repository-roles-modal.component.html',
    styleUrls: ['./manage-content-repository-roles-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ManageContentRepositoryRolesModal extends BaseModal<string[]> implements OnInit, OnDestroy {

    public readonly TableSelectAllType = TableSelectAllType;

    @Input()
    public contentRepository: ContentRepositoryBO;

    public loaded = false;
    public working = false;

    public rows: TableRow<string>[] = [];
    public columns: TableColumn<string>[] = [];
    public selected: string[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: ContentRepositoryHandlerService,
        protected i18n: I18nService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.columns = [
            {
                id: 'name',
                fieldPath: [],
                label: this.i18n.instant('common.name'),
            },
        ];
        this.subscriptions.push(combineLatest([
            this.handler.getAvailableMeshRoles(this.contentRepository.id),
            this.handler.getAssignedMeshRoles(this.contentRepository.id),
        ]).subscribe(([all, assigned]) => {
            this.rows = all.map(role => ({
                id: role,
                item: role,
            }));
            this.selected = assigned;
            this.loaded = true;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    updateSelection(selection: string[]): void {
        this.selected = selection;
    }

    updateAssignment(): void {
        this.working = true;

        this.subscriptions.push(this.handler.assignMeshRoles(this.contentRepository.id, this.selected).subscribe(roles => {
            this.closeFn(roles);
        }, () => {
            this.working = false;
            this.changeDetector.markForCheck();
        }));
    }
}
