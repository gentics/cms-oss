import { AdminUIEntityDetailRoutes, TemplateTagBO } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, TemplateTag } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TemplateTagTableLoaderOptions, TemplateTagTableLoaderService } from '../../providers';

@Component({
    selector: 'gtx-template-tag-table',
    templateUrl: './template-tag-table.component.html',
    styleUrls: ['./template-tag-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TemplateTagTableComponent
    extends BaseEntityTableComponent<TemplateTag, TemplateTagBO, TemplateTagTableLoaderOptions>
    implements OnChanges {

    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public templateId: number | string;

    protected rawColumns: TableColumn<TemplateTagBO>[] = [
        {
            id: 'name',
            label: 'templateTag.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'construct',
            label: 'templateTag.tagType',
            fieldPath: 'constructId',
        },
        {
            id: 'editableInPage',
            label: 'templateTag.editableInPage',
            fieldPath: 'editableInPage',
            align: 'center',
        },
        {
            id: 'mandatory',
            label: 'templateTag.mandatory',
            fieldPath: 'mandatory',
            align: 'center',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'templateTag';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: TemplateTagTableLoaderService,
        modalService: ModalService,
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

        if (changes.templateId) {
            this.loadTrigger.next();
        }
    }

    protected override createAdditionalLoadOptions(): TemplateTagTableLoaderOptions {
        return {
            templateId: this.templateId,
        };
    }

    protected createTableActionLoading(): Observable<TableAction<TemplateTagBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<TemplateTagBO>[] = [
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        enabled: true,
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        )
    }
}
