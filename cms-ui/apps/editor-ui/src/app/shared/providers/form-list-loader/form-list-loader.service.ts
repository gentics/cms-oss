import { inject, Injectable } from '@angular/core';
import { Form, FormListOptions, FormListResponse, PagingSortOrder } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { from, map, Observable, of, Subject, switchMap } from 'rxjs';
import { ItemListLoaderService, ItemLoadData, ListLoadOptions } from '../../../common/models';
import { FolderActionsService } from '../../../state';

abstract class BaseListLoaderService<T, O extends ListLoadOptions = ListLoadOptions> implements ItemListLoaderService<T, O> {

    protected reloadSub = new Subject<void>();

    reload$ = this.reloadSub.asObservable();

    reload(): void {
        this.reloadSub.next();
    }

    abstract loadItems(options: O): Observable<ItemLoadData<T>>;
}

interface FormListLoadOptions extends ListLoadOptions {
    external?: boolean;
}

@Injectable({
    providedIn: 'root',
})
export class FormListLoaderService extends BaseListLoaderService<Form, FormListLoadOptions> {

    private folderActions = inject(FolderActionsService);
    private client = inject(GCMSRestClientService);

    loadItems(loadOptions: FormListLoadOptions): Observable<ItemLoadData<Form>> {
        const listOptions: FormListOptions = {
            folderId: loadOptions.folderId,
            page: loadOptions.page,
            pageSize: loadOptions.pageSize,
            external: loadOptions.external,
            wastebin: loadOptions.showDeleted ? 'include' : 'exclude',
        };

        if (loadOptions.searchString) {
            listOptions.recursive = true;
            listOptions.q = loadOptions.searchString;
        }

        if (loadOptions.sortBy) {
            listOptions.sort = {
                attribute: loadOptions.sortBy as any,
                sortOrder: loadOptions.sortOrder ?? PagingSortOrder.None,
            };
        }

        if (loadOptions.package) {
            listOptions.package = loadOptions.package;
        }

        return from(this.folderActions.getItems(loadOptions.folderId, 'form', true, listOptions)).pipe(
            switchMap((res: FormListResponse) => {
                if (!loadOptions.withUsage || res.items.length === 0) {
                    return of(res);
                }

                return this.client.form.usageInTotal({
                    id: res.items.map((form) => form.id),
                    nodeId: loadOptions.nodeId,
                }).pipe(map((usageRes) => {
                    return {
                        ...res,
                        items: res.items.map((form) => {
                            return {
                                ...form,
                                usage: usageRes.infos[form.id],
                            };
                        }),
                    };
                }));
            }),
            map((res) => {
                return {
                    items: res.items,
                    totalCount: res.numItems,
                    stagingData: res.stagingStatus,
                };
            }),
        );
    }

}
