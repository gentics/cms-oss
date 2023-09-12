import {
    I18nService,
    PackageCheckTrableLoaderOptions,
    PackageCheckTrableLoaderService,
} from '@admin-ui/core';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
} from '@angular/core';
import { PackageDependencyEntity } from '@gentics/cms-models';
import { TableColumn } from '@gentics/ui-core';
import { PackageDependencyEntityBO } from '@admin-ui/common';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';

@Component({
    selector: 'gtx-dev-tool-package-trable',
    templateUrl: './dev-tool-package-trable.component.html',
    styleUrls: ['./dev-tool-package-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PackageCheckTrableComponent
    extends BaseEntityTrableComponent<PackageDependencyEntity, PackageDependencyEntityBO, PackageCheckTrableLoaderOptions>
    implements OnInit, OnChanges
{
    @Input()
    public packageName: string;

    @Input()
    public checkAll: boolean;

    public isChecked: boolean;

    private shouldReload = false;

    public rawColumns: TableColumn<PackageDependencyEntityBO>[] = [
        {
            id: 'name',
            label: 'shared.element',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'dependencyType',
            label: 'shared.type',
            fieldPath: 'dependencyType',
            sortable: true,
        },
        {
            id: 'isInPackage',
            label: 'package.consistency_check_contained',
            fieldPath: 'isInPackage',
            align: 'center',
            sortable: false,
        },
        {
            id: 'globalId',
            label: 'id',
            fieldPath: 'globalId',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: PackageCheckTrableLoaderService,
    ) {
        super(changeDetector, i18n, loader);
    }

    public override ngOnInit(): void {
        super.ngOnInit();
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
    }

    protected override createAdditionalLoadOptions(): PackageCheckTrableLoaderOptions {
        return {
            packageName: this.packageName,
            checkAll: this.checkAll,
            shouldReload: this.shouldReload,
        };
    }

    protected rebuildColumns(): void {
        const columnsToTranslate = [...this.rawColumns];
        this.columns = this.translateColumns(columnsToTranslate);
    }
}
