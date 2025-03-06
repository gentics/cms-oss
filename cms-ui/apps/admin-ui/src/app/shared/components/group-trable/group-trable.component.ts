import { CategoryInfo, GroupBO, PermissionsCategorizer, PermissionsUtils } from '@admin-ui/common';
import { GroupTrableLoaderOptions, GroupTrableLoaderService, I18nService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AccessControlledType, Group } from '@gentics/cms-models';
import { TableColumn, TrableRow } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { GroupDataService } from '../../providers/group-data/group-data.service';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';

@Component({
    selector: 'gtx-group-trable',
    templateUrl: './group-trable.component.html',
    styleUrls: ['./group-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupTrableComponent extends BaseEntityTrableComponent<Group, GroupBO, GroupTrableLoaderOptions> implements OnChanges {

    @Input()
    public permissions: boolean;

    @Input()
    public parentId: number;

    @Input()
    public parentType: AccessControlledType.NODE | AccessControlledType.FOLDER;

    @Input()
    public parentName: string;

    @Input()
    public parentHasChildren: boolean;

    @Input()
    public groupPermissionsByCategory: boolean;

    public categorizer: PermissionsCategorizer;

    protected oldCagegories: CategoryInfo[] = [];

    public rawColumns: TableColumn<GroupBO>[] = [
        {
            id: 'name',
            label: 'shared.element',
            fieldPath: 'name',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: GroupTrableLoaderService,
        protected dataService: GroupDataService,
    ) {
        super(changeDetector, i18n, loader);
        this.booleanInputs.push('permissions', 'parentHasChildren', 'groupPermissionsByCategory');
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.groupPermissionsByCategory) {
            this.categorizer = this.groupPermissionsByCategory
                ? PermissionsUtils.createCategorizerByCategoryId()
                : PermissionsUtils.createCategorizerByPermType();
            if (this.permissions) {
                this.rebuildColumns();
            }
        }
    }

    protected rebuildColumns(): void {
        const columnsToTranslate = [...this.rawColumns];
        if (this.permissions) {
            columnsToTranslate.push(...this.createPermissionColumns());
        }
        this.columns = this.translateColumns(columnsToTranslate);
    }

    protected createPermissionColumns(): TableColumn<GroupBO>[] {
        return this.categorizer.getKnownCategories().map(category => {
            return {
                id: `permission_${category.id}`,
                label: category.label,
                fieldPath: `categorizedPerms.${category.id}`,
            };
        });
    }

    override handleRowClick(row: TrableRow<GroupBO>): void {
        this.dataService.editGroupPermissions(row.item, row.item.permissionSet, this.groupPermissionsByCategory).then(didChange => {
            if (didChange) {
                this.reloadRow(row, {
                    reloadDescendants: didChange.subGroups,
                });
            }
        });
    }

    protected override onLoad(): void {
        if (this.permissions && !isEqual(this.oldCagegories, this.categorizer.getKnownCategories())) {
            this.rebuildColumns();
        }
    }

    protected override createAdditionalLoadOptions(): GroupTrableLoaderOptions {
        return {
            permissions: this.permissions,
            parentId: this.parentId,
            parentType: this.parentType,
            parentName: this.parentName,
            parentHasChildren: this.parentHasChildren,
            categorizer: this.categorizer,
        }
    }
}
