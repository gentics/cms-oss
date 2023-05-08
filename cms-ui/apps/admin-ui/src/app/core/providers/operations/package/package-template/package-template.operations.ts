import { Injectable, Injector } from '@angular/core';
import {
    Raw,
    Template,
    TemplateBO,
    TemplateFolderListRequest,
    TemplateResponse,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../../extended-entity-operations';

@Injectable()
export class PackageTemplateOperations extends ExtendedEntityOperationsBase<'template'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
    ) {
        super(injector, 'template');
    }

    /**
     * Get list of templates
     */
    getAll(options?: TemplateFolderListRequest, packageName?: string): Observable<TemplateBO<Raw>[]> {
        return this.api.devTools.getTemplates(packageName, options).pipe(
            map(res => {
                // fake entity's `id` property to enforce internal application entity uniformity
                return res.items.map(item => Object.assign(item, { id: item.name }) as TemplateBO<Raw>);
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get a single template
     */
    get(packageName: string, name: string): Observable<TemplateBO<Raw>> {
        return this.api.devTools.getTemplate(packageName, name).pipe(
            map((res: TemplateResponse) => res.template),
            // fake entity's `id` property to enforce internal application entity uniformity
            map((item: Template<Raw>) => Object.assign(item, { id: item.name })),
            this.catchAndRethrowError(),
        );
    }

}
