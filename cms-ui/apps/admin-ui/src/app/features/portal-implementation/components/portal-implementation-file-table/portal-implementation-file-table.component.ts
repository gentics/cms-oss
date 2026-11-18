import { PortalImplementationFileBO } from '@admin-ui/common';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AnyModelType, NormalizableEntityTypesMap, PortalImplementationFile, User } from '@gentics/cms-models';
import { ChangesOf, ModalService, TableColumn } from '@gentics/ui-core';
import { PortalImplementationFileTableLoaderOptions, PortalImplementationFileTableLoaderService } from '../../providers';

/**
 * Read-only table of the files contained in a portal implementation
 */
@Component({
    selector: 'gtx-portal-implementation-file-table',
    templateUrl: './portal-implementation-file-table.component.html',
    styleUrls: ['./portal-implementation-file-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PortalImplementationFileTableComponent
    extends BaseEntityTableComponent<PortalImplementationFile, PortalImplementationFileBO, PortalImplementationFileTableLoaderOptions>
    implements OnChanges {

    /** ID of the portal implementation */
    @Input()
    public portalImplementationId: string | number;

    protected rawColumns: TableColumn<PortalImplementationFileBO>[] = [
        {
            id: 'path',
            label: 'portal_implementation.file_path',
            fieldPath: 'path',
            sortable: true,
        },
        {
            id: 'mimetype',
            label: 'portal_implementation.file_mimetype',
            fieldPath: 'mimetype',
            sortable: true,
        },
        {
            id: 'filesize',
            label: 'portal_implementation.file_size',
            fieldPath: 'filesize',
            align: 'right',
            sortable: true,
        },
        {
            id: 'editor',
            label: 'portal_implementation.file_editor',
            fieldPath: 'editor',
            mapper: (editor: User) => editor ? `${editor.firstName} ${editor.lastName}` : '',
        },
        {
            id: 'edate',
            label: 'portal_implementation.file_edate',
            fieldPath: 'edate',
            sortable: true,
        },
    ];

    /** files are not stored in the entity state */
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: PortalImplementationFileTableLoaderService,
        modalService: ModalService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.portalImplementationId) {
            this.loadTrigger.next();
        }
    }

    protected override createAdditionalLoadOptions(): PortalImplementationFileTableLoaderOptions {
        return {
            portalImplementationId: this.portalImplementationId,
        };
    }
}
