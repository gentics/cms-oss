import { Injectable, Injector } from '@angular/core';
import { Form, FormListOptions, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class FormOperations extends ExtendedEntityOperationsBase<'form'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
    ) {
        super(injector, 'form');
    }

    getAll(options: FormListOptions, parentId: any): Observable<Form<Raw>[]> {
        return this.api.folders.getForms(parentId, options).pipe(
            map(res => res.items),
        );
    }

    get(entityId: number, options?: any, parentId?: any): Observable<Form<Raw>> {
        return this.api.forms.getForm(entityId).pipe(
            map(res => res.item),
        );
    }
}
