import { ImportErrorBO } from '@admin-ui/common';
import { ContentPackageOperations, I18nNotificationService, I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    SimpleChanges,
} from '@angular/core';
import {
    ImportError,
} from '@gentics/cms-models';
import { ModalService, TableColumn } from '@gentics/ui-core';
import { BehaviorSubject } from 'rxjs';
import {
    ContentPackageImportErrorTableLoaderService,
    ContentStagingImportErrorTableLoaderOptions,
} from '../../providers';

@Component({
    selector: 'gtx-content-package-import-error',
    templateUrl: './content-package-import-errors.component.html',
    styleUrls: ['./content-package-import-errors.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentPackageImportErrorTableComponent extends BaseEntityTableComponent<
ImportError, ImportErrorBO, ContentStagingImportErrorTableLoaderOptions>  implements OnChanges {

    @Input()
    public packageName: string;

    public checkResultAvailable: boolean

    public lastCheckTimestamp$: BehaviorSubject<string>;

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
    ) {
        super(changeDetector, appState, i18n, loader, modalService);

        this.subscriptions.push(
            this.loader.checkResultAvailable$.subscribe(isAvailable => {
                this.checkResultAvailable = isAvailable;
            }),
        );

        this.lastCheckTimestamp$ = this.loader.lastCheckTimestamp$;
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        if (changes.packageName) {
            this.reload();
            this.changeDetector.markForCheck();
        }
    }


    public handleCheckButtonClick(packageName: string): void {
        this.i18nNotification.show({
            message: 'content_staging.start_import__check_message',
            type: 'success',
            translationParams: {
                packageName: packageName,
            },
        });

        this.operations.importFromFileSystem(packageName, {test: true}, false).toPromise().then(_success => {
            this.reload();
            this.changeDetector.markForCheck();
        });
    }


    protected override createAdditionalLoadOptions(): ContentStagingImportErrorTableLoaderOptions {
        return {
            packageName: this.packageName,
        };
    }
}
