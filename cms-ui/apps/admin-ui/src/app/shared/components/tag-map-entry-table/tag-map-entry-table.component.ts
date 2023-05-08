import { TAGMAP_ENTRY_ATTRIBUTES_MAP, TagMapEntryBO } from '@admin-ui/common';
import { I18nService, PermissionsService, TagMapEntryTableLoaderOptions, TagMapEntryTableLoaderService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, TagmapEntry, TagmapEntryParentType, TagmapEntryPropertiesObjectType } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { debounceTime, filter, map, switchMap } from 'rxjs/operators';
import { BaseEntityTableComponent, DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { CreateTagmapEntryModalComponentMode, CreateUpdateTagmapEntryModalComponent, TagmapEntryDisplayFields } from '../create-update-tagmapentry-modal';

const EDIT_ACTION = 'edit';

@Component({
    selector: 'gtx-tag-map-entry-table',
    templateUrl: './tag-map-entry-table.component.html',
    styleUrls: ['./tag-map-entry-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagMapEntryTableComponent
    extends BaseEntityTableComponent<TagmapEntry, TagMapEntryBO, TagMapEntryTableLoaderOptions>
    implements OnChanges {

    public readonly TagmapEntryPropertiesObjectType = TagmapEntryPropertiesObjectType;
    public readonly TAGMAP_ENTRY_ATTRIBUTES_MAP = TAGMAP_ENTRY_ATTRIBUTES_MAP;

    @Input()
    public parentType: TagmapEntryParentType;

    @Input()
    public parentId: string | number;

    @Input()
    public displayFields: TagmapEntryDisplayFields;

    public canEditCR = false;

    protected rawColumns: TableColumn<TagMapEntryBO>[] = [
        {
            id: 'mapname',
            label: 'tagmapEntry.mapname',
            fieldPath: 'mapname',
            sortable: true,
        },
        {
            id: 'reserved',
            label: 'tagmapEntry.reserved',
            fieldPath: 'reserved',
            align: 'center',
        },
        {
            id: 'object',
            label: 'tagmapEntry.object',
            fieldPath: 'object',
        },
        {
            id: 'tagname',
            label: 'tagmapEntry.tagname',
            fieldPath: 'tagname',
        },
        {
            id: 'attributeType',
            label: 'tagmapEntry.attributeType',
            fieldPath: 'attributeType',
        },
        {
            id: 'targetType',
            label: 'tagmapEntry.targetType',
            fieldPath: 'targetType',
        },
        {
            id: 'optimized',
            label: 'tagmapEntry.optimized',
            fieldPath: 'optimized',
            align: 'center',
        },
        // {
        //     id: 'filesystem',
        //     label: 'tagmapEntry.filesystem',
        //     fieldPath: 'filesystem',
        //     align: 'center',
        // },
        // {
        //     id: 'foreignlinkAttribute',
        //     label: 'tagmapEntry.foreignlinkAttribute',
        //     fieldPath: 'foreignlinkAttribute',
        // },
        // {
        //     id: 'foreignlinkAttributeRule',
        //     label: 'tagmapEntry.foreignlinkAttributeRule',
        //     fieldPath: 'foreignlinkAttributeRule',
        // },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'tagmapEntry';

    protected parentIdSubject: Subject<string | number> = new BehaviorSubject(null);

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: TagMapEntryTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        )
    }

    protected override createAdditionalLoadOptions(): TagMapEntryTableLoaderOptions {
        return {
            parentType: this.parentType,
            parentId: this.parentId,
        };
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        let didTrigger = false;
        if (changes.parentType) {
            this.loadTrigger.next();
            didTrigger = true;
        }

        if (changes.parentId) {
            if (this.parentIdSubject) {
                this.parentIdSubject.next(this.parentId);
            }
            if (!didTrigger) {
                this.loadTrigger.next();
            }
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<TagMapEntryBO>[]> {
        const editPermCheck = combineLatest([
            this.actionRebuildTrigger$,
            this.parentIdSubject.asObservable(),
        ]).pipe(
            debounceTime(50),
            filter(id => id != null),
            switchMap(id => {
                const perms = this.permissions.getUserActionPermsForId('contentRepository.updateContentRepositoryInstance');
                const instancePerms = {
                    ...perms.instancePermissions,
                    instanceId: id,
                };

                return combineLatest([
                    this.permissions.checkPermissions(perms.typePermissions),
                    this.permissions.checkPermissions(instancePerms),
                ]).pipe(
                    map(([typePerm, instancePerm]) => typePerm && instancePerm),
                );
            }),
        );

        return combineLatest([
            this.actionRebuildTrigger$,
            editPermCheck,
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canEdit]) => {
                this.canEditCR = canEdit;
                const actions: TableAction<TagMapEntryBO>[] = [
                    {
                        id: EDIT_ACTION,
                        icon: 'edit',
                        type: 'primary',
                        label: this.i18n.instant('common.edit'),
                        enabled: canEdit,
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        type: 'alert',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canEdit,
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    public override async handleCreateButton(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateUpdateTagmapEntryModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            {
                displayFields: this.displayFields,
                mode: CreateTagmapEntryModalComponentMode.CREATE,
                parentType: this.parentType,
                parentId: String(this.parentId),
            },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loadTrigger.next();
    }

    public override handleAction(event: TableActionClickEvent<TagMapEntryBO>): void {
        switch (event.actionId) {
            case EDIT_ACTION:
                this.editEntry(event.item);
                return;
        }

        super.handleAction(event);
    }

    protected async editEntry(entry: TagMapEntryBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateUpdateTagmapEntryModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            {
                displayFields: this.displayFields,
                mode: CreateTagmapEntryModalComponentMode.UPDATE,
                parentType: this.parentType,
                parentId: String(this.parentId),
                value: entry as any,
            },
        );
        const updated = await dialog.open();

        if (!updated) {
            return;
        }

        this.loadTrigger.next();
    }

    override callToDeleteEntity(id: string): Promise<void> {
        return (this.loader as TagMapEntryTableLoaderService).deleteEntity(id, this.createAdditionalLoadOptions());
    }
}
