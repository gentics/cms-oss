import { createMoveActions, LanguageBO } from '@admin-ui/common';
import { I18nService, LanguageLoaderOptions, LanguageTableLoaderService, PermissionsService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { AnyModelType, Language, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BaseSortableEntityTableComponent } from '../base-sortable-entity-table/base-sortable-entity-table.component';
import { CreateLanguageModalComponent } from '../create-language-modal/create-language-modal.component';

@Component({
    selector: 'gtx-language-table',
    templateUrl: './language-table.component.html',
    styleUrls: ['./language-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageTableComponent
    extends BaseSortableEntityTableComponent<Language, LanguageBO, LanguageLoaderOptions>
    implements OnChanges {

    @Input()
    public nodeId: number;

    @Output()
    public assignToNode = new EventEmitter<void>();

    protected rawColumns: TableColumn<LanguageBO>[] = [
        {
            id: 'code',
            label: 'shared.language_code',
            fieldPath: 'code',
            sortable: true,
        },
        {
            id: 'name',
            label: 'shared.language_name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'language';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: LanguageTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.nodeId) {
            this.loadTrigger.next();
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<LanguageBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('language.deleteLanguage').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete]) => {
                const actions: TableAction<LanguageBO>[] = [];

                if (this.sorting) {
                    actions.push(...createMoveActions(this.i18n, true));
                } else {
                    actions.push({
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        enabled: canDelete,
                        multiple: true,
                        single: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): LanguageLoaderOptions {
        return {
            nodeId: this.nodeId,
        };
    }

    async handleCreateButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateLanguageModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }

    handleNodeLanguageAssignment(): void {
        this.assignToNode.emit();
    }
}
