import { Injectable, Injector } from '@angular/core';
import {
    MarkupLanguageBO,
    MarkupLanguageListResponse,
    Raw
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class MarkupLanguageOperations extends ExtendedEntityOperationsBase<'markupLanguage'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
    ) {
        super(injector, 'markupLanguage');
    }

    /**
     * Get a list of all markuplanguages and adds them to the AppState.
     */
    getAll(): Observable<MarkupLanguageBO<Raw>[]> {
        return this.api.markuplanguage.getMarkupLanguages().pipe(
            map((res: MarkupLanguageListResponse) => {
                return res.items.map(item => item as any as MarkupLanguageBO<Raw>);
            }),
            tap((items: MarkupLanguageBO<Raw>[]) => this.entityManager.addEntities('markupLanguage', items)),
            this.catchAndRethrowError(),
        );
    }

    get(): Observable<MarkupLanguageBO<Raw>> {
        throw new Error('Not implemented.');
    }
}
