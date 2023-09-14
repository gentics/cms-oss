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

    public checkAll = false;

    private triggerNewCheck = false;

    public rawColumns: TableColumn<PackageDependencyEntityBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
        },
        {
            id: 'dependencyType',
            label: 'shared.type',
            fieldPath: 'dependencyType',
        },
        {
            id: 'isInPackage',
            label: 'package.consistency_check_contained',
            fieldPath: 'isContained',
            align: 'center',
        },
        {
            id: 'globalId',
            label: 'Id',
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


    protected override createAdditionalLoadOptions(): PackageCheckTrableLoaderOptions {
        return {
            packageName: this.packageName,
            checkAll: this.checkAll,
            triggerNewCheck: this.triggerNewCheck,
        };
    }

    protected rebuildColumns(): void {
        const columnsToTranslate = [...this.rawColumns];
        this.columns = this.translateColumns(columnsToTranslate);
    }

    public handleLoadButtonClick(): void {
        this.triggerNewCheck = true;
        this.loadRootElements();
    }

}
