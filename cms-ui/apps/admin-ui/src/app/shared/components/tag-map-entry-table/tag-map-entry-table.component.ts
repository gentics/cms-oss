import { TAGMAP_ENTRY_ATTRIBUTES_MAP, TagMapEntryBO } from '@admin-ui/common';
import { I18nService, PermissionsService, TagMapEntryTableLoaderOptions, TagMapEntryTableLoaderService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import {
    AnyModelType,
    NormalizableEntityTypesMap,
    TagmapEntry,
    TagmapEntryError,
    TagmapEntryParentType,
    TagmapEntryPropertiesObjectType,
} from '@gentics/cms-models';
import { ChangesOf, ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subject, combineLatest } from 'rxjs';
import { debounceTime, filter, map, switchMap } from 'rxjs/operators';
import { BaseEntityTableComponent, DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { CreateTagmapEntryModalComponentMode, CreateUpdateTagmapEntryModalComponent, TagmapEntryDisplayFields } from '../create-update-tagmapentry-modal';

const EDIT_ACTION = 'edit';
const FRAGMENT_COLUMN_ID = 'fragmentName';

function mapObjectType(row: TagMapEntryBO): number {
    return row.object || row.objType;
}

@Component({
    selector: 'gtx-tag-map-entry-table',
    templateUrl: './tag-map-entry-table.component.html',
    styleUrls: ['./tag-map-entry-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
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

    @Input()
    public errors: TagmapEntryError[] = [];

    @Input()
    public showFragments = false;

    /** Event whenever one or more entries have been created, updated, or deleted. */
    @Output()
    public entriesChange = new EventEmitter<void>();

    public canEditCR = false;
    public errorMap: Record<string, string[]> = {};

    protected rawColumns: TableColumn<TagMapEntryBO>[] = [
        {
            id: 'mapname',
            label: 'tagmapEntry.mapname',
            fieldPath: 'mapname',
            sortable: true,
        },
        {
            id: FRAGMENT_COLUMN_ID,
            label: 'tagmapEntry.fragmentname',
            fieldPath: 'fragmentName',
            sortable: true,
        },
        {
            id: 'object',
            label: 'tagmapEntry.object',
            sortable: true,
            mapper: mapObjectType,
        },
        {
            id: 'tagname',
            label: 'tagmapEntry.tagname',
            fieldPath: 'tagname',
            sortable: true,
        },
        {
            id: 'attributeType',
            label: 'tagmapEntry.attributeType',
            fieldPath: 'attributeType',
            sortable: true,
        },
        {
            id: 'targetType',
            label: 'tagmapEntry.targetType',
            fieldPath: 'targetType',
            sortable: true,
        },
        {
            id: 'reserved',
            label: 'tagmapEntry.reserved',
            fieldPath: 'reserved',
            align: 'center',
        },
        {
            id: 'optimized',
            label: 'tagmapEntry.optimized',
            fieldPath: 'optimized',
            align: 'center',
            sortable: true,
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
            withFragments: this.showFragments,
        };
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.errors) {
            this.rebuildErrorMap();
        }

        let shouldTrigger = false;
        if (changes.parentType) {
            shouldTrigger = true;
        }

        if (changes.parentId) {
            if (this.parentIdSubject) {
                this.parentIdSubject.next(this.parentId);
            }
            shouldTrigger = true;
        }

        if (changes.showFragments) {
            this.rebuildColumns();
            shouldTrigger = true;
        }

        if (shouldTrigger) {
            this.loadTrigger.next();
        }
    }

    protected override rebuildColumns(): void {
        // Setup columns with the translated labels
        let cols = this.rawColumns;
        if (!this.showFragments) {
            cols = cols.filter(col => col.id !== FRAGMENT_COLUMN_ID);
        }
        this.columns = this.translateColumns(cols);
    }

    protected rebuildErrorMap(): void {
        this.errorMap = (this.errors || []).reduce((acc, err) => {
            err.entries.forEach(id => {
                if (!Array.isArray(acc[id])) {
                    acc[id] = [];
                }
                acc[id].push(err.description);
            });
            return acc;
        }, {} as Record<string, string[]>);
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
                        enabled: (item) => item && !item.fragmentName && canEdit,
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        type: 'alert',
                        label: this.i18n.instant('shared.delete'),
                        enabled: (item) => {
                            return (item == null || (!item.reserved && !item.fragmentName)) && canEdit;
                        },
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

        this.entriesChange.emit();
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
                tagmapId: entry.id,
                reserved: entry.reserved ?? false,
            },
        );
        const updated = await dialog.open();

        if (!updated) {
            return;
        }

        this.entriesChange.emit();
        this.loadTrigger.next();
    }

    override callToDeleteEntity(id: string): Promise<void> {
        return (this.loader as TagMapEntryTableLoaderService).deleteEntity(id, this.createAdditionalLoadOptions()).then(() => {
            this.entriesChange.emit();
        });
    }
}
