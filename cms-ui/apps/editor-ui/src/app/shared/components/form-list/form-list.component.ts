import { ChangeDetectionStrategy, ChangeDetectorRef, Component, input, OnInit } from '@angular/core';
import { Form } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { isEqual, pick } from 'lodash-es';
import { distinctUntilChanged, map, Observable } from 'rxjs';
import { ItemLoadData, ItemsInfo } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { ApplicationStateService } from '../../../state';
import { FormListLoaderService } from '../../providers/form-list-loader/form-list-loader.service';
import { BaseItemListComponent } from '../base-item-list/base-item-list.component';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';

type ListUserSettings = Pick<ItemsInfo, 'itemsPerPage' | 'sortBy' | 'sortOrder'>;

@Component({
    selector: 'gtx-form-list',
    templateUrl: './form-list.component.html',
    styleUrls: ['./form-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormListComponent extends BaseItemListComponent<Form> implements OnInit {

    public readonly external = input.required<boolean>();

    constructor(
        changeDetector: ChangeDetectorRef,
        errorHandler: ErrorHandler,
        appState: ApplicationStateService,
        public client: GCMSRestClientService,
        protected loader: FormListLoaderService,
        protected userSettings: UserSettingsService,
    ) {
        super(changeDetector, errorHandler, appState);
    }

    public ngOnInit(): void {
        super.ngOnInit();

        // Register this list to the reload mechanic of the service, so other components can actually reload this list if needed
        this.subscriptions.push(this.loader.reload$.subscribe(() => {
            this.reload();
        }));

        // Currently user settings are managed via the global store, as these actually make sense to be there

        this.subscriptions.push(this.appState.select((state) => state.folder.forms.displayFields).pipe(
            distinctUntilChanged(isEqual),
        ).subscribe((displayFields: string[]) => {
            if (!Array.isArray(displayFields)) {
                displayFields = [];
            }

            // We need a reload when the usage has been added to the display fields, as these are loaded explicitly for the items.
            const needsReload = !this.displayFields().includes('usage') && displayFields.includes('usage');
            this.displayFields.set(displayFields);

            if (needsReload) {
                this.reload();
            }
        }));

        this.subscriptions.push(this.appState.select((state) => state.folder.forms).pipe(
            distinctUntilChanged(isEqual),
            map((formState: ItemsInfo) => {
                return pick(formState, 'itemsPerPage', 'sortBy', 'sortOrder');
            }),
            distinctUntilChanged<ListUserSettings>(isEqual),
        ).subscribe((formState) => {
            this.sortBy.set(formState.sortBy);
            this.sortOrder.set(formState.sortOrder as any);
            this.pageSize.set(formState.itemsPerPage);
            this.reload();
        }));
    }

    public loadItems(
        folderId: number,
        nodeId: number,
        page: number,
        pageSize: number,
    ): Observable<ItemLoadData<Form>> {
        return this.loader.loadItems({
            folderId: folderId,
            nodeId: nodeId,
            recursive: !!this.searchTerm(),

            page: page,
            pageSize: pageSize,

            showDeleted: this.showDeleted(),
            searchString: this.searchTerm(),

            sortBy: this.sortBy(),
            sortOrder: this.sortOrder(),

            external: this.external(),
            withUsage: this.displayFields().includes('usage'),
        });
    }

    public updatePageSize(pageSize: number): void {
        // Always reset the pagination. We could calculate where exactly this is supposed to end up,
        // but for now this is good enough.
        this.currentPage.set(1);
        this.userSettings.setItemsPerPage('form', pageSize);
    }
}
