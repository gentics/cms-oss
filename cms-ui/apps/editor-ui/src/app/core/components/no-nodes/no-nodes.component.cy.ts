// import { STATE_MODULES } from '@editor-ui/app/state/modules/state-modules';
// import { ApplicationStateService } from '@editor-ui/app/state/providers/application-state/application-state.service';
// import { FolderActionsService } from '@editor-ui/app/state/providers/folder-actions/folder-actions.service';
// import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
// import { AnyModelType, Node } from '@gentics/cms-models';
// import { NgxsModule } from '@ngxs/store';
// import { I18nService } from '../../providers/i18n/i18n.service';
// import { NavigationService } from '../../providers/navigation/navigation.service';
// import { NoNodesComponent } from './no-nodes.component';

// class MockI18nService implements Partial<I18nService> {
//     translate(key: string | string[], params?: any): string {
//         return Array.isArray(key) ? key[0] : key;
//     }
// }

// class MockFolderActionsService implements Partial<FolderActionsService> {
//     resolveDefaultNode(): Node<AnyModelType> {
//         return null;
//     }
// }

// class MockNavigationService implements Partial<NavigationService> {}

describe('NoNodesComponent', () => {

    xit('should render the content correctly', () => {
        // FIXME: Editor UI has "broken" imports apparently, which loop around all the time,
        // making it impossible to execute the component-tests for whatever reason.
        // Needs to be looked at in more detail.

        // cy.mount(NoNodesComponent, {
        //     imports: [
        //         NgxsModule.forRoot(STATE_MODULES),
        //     ],
        //     providers: [
        //         { provider: ApplicationStateService, useClass: TestApplicationState },
        //         { provider: I18nService, useClass: MockI18nService },
        //         { provider: FolderActionsService, useClass: MockFolderActionsService },
        //         { provider: NavigationService, useClass: MockNavigationService },
        //     ],
        // }).then(res => {
        //     const injector = res.fixture.componentRef.injector;
        //     const appState = injector.get(ApplicationStateService) as TestApplicationState;
        //     appState.mockState({
        //         folder: {
        //             nodesLoaded: true,
        //         },
        //         auth: {
        //             isAdmin: true,
        //         },
        //     });
        // });

        // cy.get('.admin-button-link')
        //     .should('exist');
    });
});
