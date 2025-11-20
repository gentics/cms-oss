import { CategoryInfo, PermissionsCategorizer, PermissionsSetBO, PermissionsUtils } from '@admin-ui/common';
import { PermissionsTrableLoaderOptions, PermissionsTrableLoaderService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { AccessControlledType, Group, PermissionsSet } from '@gentics/cms-models';
import { TableColumn, TrableRow } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { isEqual } from 'lodash-es';
import { GroupDataService } from '../../providers/group-data/group-data.service';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';

@Component({
    selector: 'gtx-permissions-trable',
    templateUrl: './permissions-trable.component.html',
    styleUrls: ['./permissions-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PermissionsTrableComponent
    extends BaseEntityTrableComponent<PermissionsSet, PermissionsSetBO, PermissionsTrableLoaderOptions>
    implements OnInit, OnChanges {

    @Input()
    public group: Group;

    @Input()
    public parentId: number;

    @Input()
    public parentType: AccessControlledType;

    @Input()
    public parentName: string;

    @Input()
    public parentHasChildren: boolean;

    @Input()
    public groupPermissionsByCategory: boolean;

    public categorizer: PermissionsCategorizer;

    protected oldCagegories: CategoryInfo[] = [];

    public rawColumns: TableColumn<PermissionsSetBO>[] = [
        {
            id: 'label',
            label: 'shared.element',
            fieldPath: 'label',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: PermissionsTrableLoaderService,
        protected dataService: GroupDataService,
    ) {
        super(changeDetector, i18n, loader);
        this.booleanInputs.push('parentHasChildren');
    }

    public override ngOnInit(): void {
        this.setupCategorizer(true);
        super.ngOnInit();
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        if (changes.groupPermissionsByCategory) {
            this.setupCategorizer(!changes.groupPermissionsByCategory.firstChange);
        }
    }

    protected setupCategorizer(rebuid: boolean = false): void {
        this.categorizer = this.groupPermissionsByCategory
            ? PermissionsUtils.createCategorizerByCategoryId()
            : PermissionsUtils.createCategorizerByPermType();
        if (rebuid) {
            this.rebuildColumns();
        }
    }

    protected override createAdditionalLoadOptions(): PermissionsTrableLoaderOptions {
        return {
            group: this.group,
            parentId: this.parentId,
            parentType: this.parentType,
            parentName: this.parentName,
            parentHasChildren: this.parentHasChildren,
            categorizer: this.categorizer,
        };
    }

    override handleRowClick(row: TrableRow<PermissionsSetBO>): void {
        this.dataService.editGroupPermissions(row.item.group, row.item, this.groupPermissionsByCategory).then((didChange) => {
            if (didChange) {
                this.reloadRow(row, {
                    reloadDescendants: didChange.subObjects,
                });
            }
        });
    }

    protected override onLoad(): void {
        if (!isEqual(this.oldCagegories, this.categorizer.getKnownCategories())) {
            this.rebuildColumns();
        }
    }

    protected rebuildColumns(): void {
        const columnsToTranslate = [...this.rawColumns, ...this.createPermissionColumns()];
        this.columns = this.translateColumns(columnsToTranslate);
    }

    protected createPermissionColumns(): TableColumn<PermissionsSetBO>[] {
        return this.categorizer.getKnownCategories().map((category) => {
            return {
                id: `permission_${category.id}`,
                label: category.label,
                fieldPath: ['categorized', category.id],
            };
        });
    }
}
