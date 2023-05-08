import { LanguageOperations } from '@admin-ui/core';
import { LanguageDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an component with changable elements.
 */
@Injectable()
export class CanActivateLanguageGuard extends AbstractCanActivateEntityGuard<'language', LanguageOperations> {

    constructor(
        languageData: LanguageDataService,
        appState: AppStateService,
    ) {
        super(
            'language',
            languageData,
            appState,
        );
    }

}
