import { ImportErrorBO } from '@admin-ui/common';
import { ContentPackageOperations, I18nNotificationService, I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
} from '@angular/core';
import {
    ContentPackageBO,
    ContentPackageImportError,
} from '@gentics/cms-models';
import { ChangesOf, ModalService, TableColumn } from '@gentics/ui-core';
import {
    ContentPackageImportErrorTableLoaderService,
    ContentPackageTableLoaderService,
    ContentStagingImportErrorTableLoaderOptions,
} from '../../providers';

@Component({
    selector: 'gtx-content-package-import-error-table',
    templateUrl: './content-package-import-error-table.component.html',
    styleUrls: ['./content-package-import-error-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ContentPackageImportErrorTableComponent
    extends BaseEntityTableComponent<ContentPackageImportError, ImportErrorBO, ContentStagingImportErrorTableLoaderOptions>
    implements OnInit, OnChanges {

    @Input()
    public contentPackage: ContentPackageBO;

    @Output()
    public reloadPackage = new EventEmitter<void>();

    public checkResultAvailable: boolean

    public lastCheckTimestamp: string;

    protected entityIdentifier: any = 'contentPackageImport';

    protected rawColumns: TableColumn<ImportErrorBO>[] = [
        {
            id: 'globalId',
            label: 'shared.object_id',
            fieldPath: 'globalId',
        },
        {
            id: 'error',
            label: 'shared.description',
            fieldPath: 'error',
        },
        {
            id: 'recommendation',
            label: 'shared.empty',
            fieldPath: 'recommendation',
        },
        {
            id: 'kind',
            label: 'shared.type',
            fieldPath: 'kind',
        },
        {
            id: 'path',
            label: 'shared.path',
            fieldPath: 'path',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        protected loader: ContentPackageImportErrorTableLoaderService,
        private operations: ContentPackageOperations,
        private i18nNotification: I18nNotificationService,
        modalService: ModalService,
        private contentPackageLoader: ContentPackageTableLoaderService,
    ) {
        super(changeDetector, appState, i18n, loader, modalService);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(
            this.loader.checkResultAvailable$.subscribe(isAvailable => {
                this.checkResultAvailable = isAvailable;
                this.changeDetector.markForCheck();
            }),
            this.loader.lastCheckTimestamp$.subscribe(timestamp => {
                this.lastCheckTimestamp = timestamp;
                this.changeDetector.markForCheck();
            }),
        );
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.contentPackage) {
            this.reload();
        }
    }

    public handleCheckButtonClick(): void {
        this.i18nNotification.show({
            message: 'content_staging.start_import__check_message',
            type: 'success',
            translationParams: {
                packageName: this.contentPackage.name,
            },
        });

        this.subscriptions.push(this.operations.importFromFileSystem(this.contentPackage.name, {test: true, wait: 1}, false).subscribe(() => {
            this.reloadWithPackage();
            this.contentPackageLoader.reload();
            this.changeDetector.markForCheck();
        }));
    }

    public reloadWithPackage(): void {
        this.reload();
        this.reloadPackage.emit();
    }

    protected override createAdditionalLoadOptions(): ContentStagingImportErrorTableLoaderOptions {
        return {
            packageName: this.contentPackage?.name,
        };
    }
}
