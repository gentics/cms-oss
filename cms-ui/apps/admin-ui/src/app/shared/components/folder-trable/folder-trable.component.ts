import { FolderBO } from '@admin-ui/common';
import { FolderTrableLoaderOptions, FolderTrableLoaderService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Folder } from '@gentics/cms-models';
import { TableAction, TableColumn } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';

@Component({
    selector: 'gtx-folder-trable',
    templateUrl: './folder-trable.component.html',
    styleUrls: ['./folder-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FolderTrableComponent extends BaseEntityTrableComponent<Folder, FolderBO, FolderTrableLoaderOptions> {

    @Input()
    public rootId: number;

    @Input()
    public includeRoot: boolean;

    @Input()
    public actions: TableAction<FolderBO>[] = [];

    public rawColumns: TableColumn<FolderBO>[] = [
        {
            id: 'name',
            label: 'shared.element',
            fieldPath: 'name',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: FolderTrableLoaderService,
    ) {
        super(changeDetector, i18n, loader);
        this.booleanInputs.push('includeRoot');
    }

    protected override createAdditionalLoadOptions(): FolderTrableLoaderOptions {
        return {
            rootId: this.rootId,
            includeRoot: this.includeRoot,
        };
    }
}
