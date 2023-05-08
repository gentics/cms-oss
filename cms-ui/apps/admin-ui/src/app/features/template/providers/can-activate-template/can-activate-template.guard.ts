import { TemplateOperations } from '@admin-ui/core';
import { TemplateDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { EntityIdType, Raw, TemplateBO } from '@gentics/cms-models';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateTemplateGuard extends AbstractCanActivateEntityGuard<'template', TemplateOperations> {
    constructor(
        dataService: TemplateDataService,
        appState: AppStateService,
        protected entityOperations: TemplateOperations,
    ) {
        super(
            'template',
            dataService,
            appState,
        );
    }

    protected loadEntityById(id: EntityIdType): Promise<TemplateBO<Raw>> {
        return this.entityOperations.get(String(id), { construct: true, update: true }).toPromise();
    }

}
