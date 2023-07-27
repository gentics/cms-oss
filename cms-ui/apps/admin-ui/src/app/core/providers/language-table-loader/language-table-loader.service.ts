import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_NEW_SORT_ORDER,
    BO_ORIGINAL_SORT_ORDER,
    BO_PERMISSIONS,
    EntityPageResponse,
    LanguageBO,
    TableLoadOptions,
} from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Language, LanguageListResponse, NodeLanguageListRequest } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { LanguageHandlerService } from '../language-handler/language-handler.service';

export interface LanguageLoaderOptions {
    nodeId?: number;
}

@Injectable()
export class LanguageTableLoaderService extends BaseTableLoaderService<Language, LanguageBO, LanguageLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected handler: LanguageHandlerService,
    ) {
        super('language', entityManager, appState);
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: LanguageLoaderOptions,
    ): Observable<EntityPageResponse<LanguageBO>> {
        let loader: Observable<LanguageListResponse>;

        if (additionalOptions?.nodeId) {
            // When loading via node id, we want to load all languages, as they are used for sorting.
            // Sorting languages with a pagination is however not neatly doable, which is why it's just one huge list instead.
            const loadOptions: NodeLanguageListRequest = {};
            if (options.query) {
                loadOptions.q = options.query;
            }

            loader = this.api.node.getNodeLanguageList(additionalOptions.nodeId, loadOptions);
        } else {
            const loadOptions = this.createDefaultOptions(options);
            loader = this.api.language.getLanguages(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map((lang, index) => this.mapToBusinessObject(lang, index));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId).toPromise();
    }

    public mapToBusinessObject(lang: Language, index: number = -1): LanguageBO {
        return {
            ...lang,
            [BO_ID]: String(lang.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.handler.displayName(lang),
            [BO_ORIGINAL_SORT_ORDER]: index,
            [BO_NEW_SORT_ORDER]: index,
        };
    }

    public override mapToTableRow(bo: LanguageBO): TableRow<LanguageBO> {
        return {
            id: bo[BO_ID],
            hash: bo[BO_NEW_SORT_ORDER],
            item: bo,
        };
    }
}
