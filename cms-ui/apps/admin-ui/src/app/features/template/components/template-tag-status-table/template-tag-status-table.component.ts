import { AdminUIEntityDetailRoutes, TagStatusBO } from '@admin-ui/common';
import { I18nNotificationService, I18nService, TemplateTagStatusOperations } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, TagStatus } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TemplateTagStatusTableLoaderOptions, TemplateTagStatusTableLoaderService } from '../../providers';

const SYNC_ACTION = 'syncTag';

@Component({
    selector: 'gtx-template-tag-status-table',
    templateUrl: './template-tag-status-table.component.html',
    styleUrls: ['./template-tag-status-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TemplateTagStatusTableComponent
    extends BaseEntityTableComponent<TagStatus, TagStatusBO, TemplateTagStatusTableLoaderOptions>
    implements OnChanges {

    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public templateId: number | string;

    protected rawColumns: TableColumn<TagStatusBO>[] = [
        {
            id: 'name',
            label: 'templateTag.name',
            fieldPath: 'name',
        },
        {
            id: 'construct',
            label: 'templateTag.constructName',
            fieldPath: 'constructName',
        },
        {
            id: 'inSync',
            label: 'templateTag.inSync',
            fieldPath: 'inSync',
            align: 'right',
        },
        {
            id: 'outOfSync',
            label: 'templateTag.outOfSync',
            fieldPath: 'outOfSync',
            align: 'right',
        },
        {
            id: 'missing',
            label: 'templateTag.missing',
            fieldPath: 'templateTag.missing',
            align: 'right',
        },
        {
            id: 'incompatible',
            label: 'templateTag.incompatible',
            fieldPath: 'incompatible',
            align: 'right',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        protected loader: TemplateTagStatusTableLoaderService,
        modalService: ModalService,
        protected entityOperations: TemplateTagStatusOperations,
        protected notification: I18nNotificationService,
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

    protected override createAdditionalLoadOptions(): TemplateTagStatusTableLoaderOptions {
        return {
            templateId: this.templateId,
        };
    }

    protected override createTableActionLoading(): Observable<TableAction<TagStatusBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<TagStatusBO>[] = [
                    {
                        id: SYNC_ACTION,
                        icon: 'sync',
                        label: this.i18n.instant('template.sync'),
                        type: 'primary',
                        single: true,
                        multiple: true,
                        enabled: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public override handleAction(event: TableActionClickEvent<TagStatusBO>): void {
        switch (event.actionId) {
            case SYNC_ACTION:
                this.askUserToSyncTags(this.loader.getEntitiesByIds(this.getAffectedEntityIds(event)));
                return;
        }

        super.handleAction(event);
    }

    protected async askUserToSyncTags(tags: TagStatus[]): Promise<void> {
        if (this.disabled || tags.length < 1) {
            return;
        }

        const dialog = await this.modalService.dialog({
            title: this.i18n.instant('modal.confirm_template_tag_sync_title'),
            body: this.i18n.instant('modal.confirm_template_tag_sync_body', {
                names: this.i18n.join(tags.map(t => t.name), {
                    quoted: true,
                    withLast: true,
                }),
            }),
            buttons: [
                {
                    label: this.i18n.instant('common.cancel_button'),
                    returnValue: false,
                    type: 'secondary',
                },
                {
                    label: this.i18n.instant('template.sync_compatible'),
                    returnValue: 'compatible',
                    type: 'default',
                },
                {
                    label: this.i18n.instant('template.sync_all'),
                    returnValue: 'all',
                    type: 'warning',
                },
            ],
        });
        const option = await dialog.open();

        switch (option) {
            case 'compatible':
                await this.synchronizeToPages(tags, false);
                break;
            case 'all':
                await this.synchronizeToPages(tags, true);
                break;
            default:
                break;
        }
    }

    protected async synchronizeToPages(tags: TagStatus[], forceSync: boolean): Promise<void> {
        if (this.disabled) {
            return;
        }

        try {
            await this.entityOperations.synchronizeTags(this.templateId, tags.map(tag => tag.name), forceSync).toPromise();
        } catch (error) {
            this.notification.show({
                message: 'templateTag.sync_error',
                type: 'alert',
            });
            return;
        }

        if (tags.length === 1) {
            this.notification.show({
                message: 'templateTag.sync_success_singular',
                translationParams: {
                    name: tags[0].name,
                },
                type: 'success',
            });
        } else if (tags.length <= 3) {
            this.notification.show({
                message: 'templateTag.sync_success_plural',
                translationParams: {
                    names: this.i18n.join(tags.map(tag => tag.name), {
                        quoted: true,
                        withLast: true,
                    }),
                },
                type: 'success',
            });
        } else {
            this.notification.show({
                message: 'templateTag.sync_success_many',
                translationParams: {
                    amount: tags.length,
                },
                type: 'success',
            });
        }
    }
}
