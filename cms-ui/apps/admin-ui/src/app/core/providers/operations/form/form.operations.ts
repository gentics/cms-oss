import { Injectable, Injector } from '@angular/core';
import { Form, FormListOptions } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class FormOperations extends ExtendedEntityOperationsBase<'form'> {

    constructor(
        injector: Injector,
        private client: GCMSRestClientService,
    ) {
        super(injector, 'form');
    }

    getAll(options: FormListOptions, parentId: any): Observable<Form[]> {
        return this.client.form.list({
            ...options,
            folderId: parentId,
        }).pipe(
            map((res) => res.items),
        );
    }

    get(entityId: number, options?: any, parentId?: any): Observable<Form> {
        return this.client.form.get(entityId).pipe(
            map((res) => res.item),
        );
    }
}
