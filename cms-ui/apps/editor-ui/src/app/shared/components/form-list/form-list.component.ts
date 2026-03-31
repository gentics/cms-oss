import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BaseItemListComponent, ItemLoadData } from '../base-item-list/base-item-list.component';
import { Form, PagingSortOrder } from '@gentics/cms-models';
import { map, Observable } from 'rxjs';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ApplicationStateService } from '../../../state';
import { ChangesOf } from '@gentics/ui-core';

@Component({
    selector: 'gtx-form-list',
    templateUrl: './form-list.component.html',
    styleUrls: ['./form-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormListComponent extends BaseItemListComponent<Form> implements OnInit {

    private activeLanguageId: number;

    constructor(
        changeDetector: ChangeDetectorRef,
        errorHandler: ErrorHandler,
        appState: ApplicationStateService,
        public client: GCMSRestClientService,
    ) {
        super(changeDetector, errorHandler, appState);
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.appState.select((state) => state.folder.activeFormLanguage).subscribe((id) => {
            this.activeLanguageId = id;
            this.determineActiveLanguage();
            this.changeDetector.markForCheck();
        }));

        // Selection is currently deeply woven into the state, therefore we'll load it from there for the time being
        this.subscriptions.push(this.appState.select((state) => state.folder.forms.selected).subscribe((selectedFormIds) => {
            this.selection.set(selectedFormIds);
            this.updateItemsInfo();
            this.changeDetector.markForCheck();
        }));
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.nodeLanguages) {
            this.determineActiveLanguage();
        }
    }

    public loadItems(
        folderId: number,
        _nodeId: number,
        page: number,
        pageSize: number,
    ): Observable<ItemLoadData<Form>> {
        return this.client.form.list({
            folderId: folderId,
            page: page,
            pageSize: pageSize,
            wastebin: this.showDeleted ? 'include' : 'exclude',
            q: this.searchTerm || '',
            sort: {
                sortOrder: PagingSortOrder.Asc,
                attribute: 'name',
            },
            recursive: !!this.searchTerm,
        }).pipe(
            map((res) => {
                return {
                    items: res.items,
                    totalCount: res.numItems,
                };
            }),
        );
    }

    private determineActiveLanguage(): void {
        this.activeLanguage = (this.nodeLanguages() || []).find((lang) => lang.id === this.activeLanguageId);
    }
}
