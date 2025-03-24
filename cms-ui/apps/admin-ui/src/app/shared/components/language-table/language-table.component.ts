import { EditableEntity, LanguageBO } from '@admin-ui/common';
import { createMoveActions, I18nService, PermissionsService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { AnyModelType, Language, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    LanguageLoaderOptions,
    LanguageTableLoaderService,
} from '../../providers/language-table-loader/language-table-loader.service';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BaseSortableEntityTableComponent } from '../base-sortable-entity-table/base-sortable-entity-table.component';

export const UNASSIGN_ACTION = 'unassign';

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
    protected focusEntityType = EditableEntity.LANGUAGE;
    protected languageLoader: LanguageTableLoaderService;

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
        this.languageLoader = loader;
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
                    actions.push({
                        id: UNASSIGN_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('shared.unassign_languages_from_node'),
                        type: 'alert',
                        enabled: true,
                        multiple: true,
                        single: true,
                    });
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

    handleNodeLanguageAssignment(): void {
        this.assignToNode.emit();
    }

    override handleAction(event: TableActionClickEvent<LanguageBO>): void {
        super.handleAction(event);

        switch (event.actionId) {
            case UNASSIGN_ACTION:
                this.languageLoader.unassignLanguageFromNode(this.nodeId, event.item.id);
                this.languageLoader.reload();
                break;
        }

    }
}
