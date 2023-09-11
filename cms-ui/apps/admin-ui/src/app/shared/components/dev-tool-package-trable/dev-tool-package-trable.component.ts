import {
    I18nService,
    PackageCheckLoaderOptions,
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
import { PackageDependency } from '@gentics/cms-models';
import { TableColumn } from '@gentics/ui-core';
import { PackageDependencyBO } from '@admin-ui/common';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';

@Component({
    selector: 'gtx-dev-tool-package-trable',
    templateUrl: './dev-tool-package-trable.component.html',
    styleUrls: ['./dev-tool-package-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PackageCheckTrableComponent
    extends BaseEntityTrableComponent<
    PackageDependency,
    PackageDependencyBO,
    PackageCheckLoaderOptions
    >
    implements OnInit, OnChanges
{
    @Input()
    public packageName: string;

    public rawColumns: TableColumn<PackageDependencyBO>[] = [
        {
            id: 'globalId',
            label: 'shared.element',
            fieldPath: 'referenceDependencies',
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

    protected override createAdditionalLoadOptions(): PackageCheckLoaderOptions {
        return {
            packageName: this.packageName,
        };
    }

    protected override onLoad(): void {}

    protected rebuildColumns(): void {
        const columnsToTranslate = [...this.rawColumns];
        this.columns = this.translateColumns(columnsToTranslate);
    }
}
