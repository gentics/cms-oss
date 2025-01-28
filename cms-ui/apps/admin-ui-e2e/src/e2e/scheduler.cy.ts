import '@gentics/e2e-utils/commands';
import { AUTH_ADMIN } from '../support/app.po';
import { EntityImporter, IMPORT_ID, IMPORT_TYPE, IMPORT_TYPE_TASK, ScheduleTaskImportData, TestSize } from '@gentics/e2e-utils';
import { AccessControlledType } from '@gentics/cms-models';

const EXAMPLE_TASK_ONE: ScheduleTaskImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_TASK,
    [IMPORT_ID]: 'taskExampleOne',

    name: 'taskOne',
    command: 'sleep.sh'
};

describe('Scheduler Module', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_MODULE = '@module';
    const ALIAS_TAB_CONTENT = '@tabContent';
    const ALIAS_TASK_TABLE_LOAD_REQ = '@taskTableLoadReq';
    const ALIAS_TASK_SAVE_REQ = '@taskSaveReq';

    before(() => {
        cy.muteXHR();
        cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL));
    });

    beforeEach(() => {
        cy.muteXHR();
        // If this client isn't recreated for WHATEVER reason, the CMS gives back a 401 for importer requests.
        IMPORTER.client = null;

        cy.wrap(null, { log: false })
            .then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            })
            .then(() => {
                return cy.wrap(IMPORTER.syncPackages(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            })
            .then(() => {
                return cy.wrap(IMPORTER.importData([
                    EXAMPLE_TASK_ONE,
                ], TestSize.NONE), { log: false, timeout: 60_000 });
            });

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);

        cy.intercept({
            method: 'GET',
            pathname: '/rest/scheduler/task',
        }, req => {
            req.alias = ALIAS_TASK_TABLE_LOAD_REQ;
        });

        cy.navigateToModule('scheduler', AccessControlledType.SCHEDULER)
            .as(ALIAS_MODULE);
    });

    describe('Tasks', () => {
        const ALIAS_TABLE = '@table';
        const ALIAS_FORM = '@form';

        beforeEach(() => {
            // Wait for the table to finish loading
            cy.wait(ALIAS_TASK_TABLE_LOAD_REQ);

            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get(ALIAS_MODULE)
                .find('> gtx-tabs')
                .selectTab('tasks')
                .as(ALIAS_TAB_CONTENT)
                .find('gtx-table')
                .as(ALIAS_TABLE);
        });

        it('should properly save modified tasks', () => {
            const TASK_ID = `${IMPORTER.get(EXAMPLE_TASK_ONE)?.id}`;
            const CHANGE_TASK_NAME = 'New Task Name';

            cy.get(ALIAS_TABLE)
                .findTableRow(TASK_ID)
                .click({ force: true })
                .should('have.class', 'active');

            cy.getDetailView()
                .find('gtx-schedule-task-properties')
                .should('exist')
                .as(ALIAS_FORM);

            cy.get(ALIAS_FORM)
                .find('[formcontrolname="name"] input')
                .type(`{selectall}{del}${CHANGE_TASK_NAME}`);

            cy.intercept({
                method: 'PUT',
                pathname: `/rest/scheduler/task/${TASK_ID}`,
            }, req => {
                req.alias = ALIAS_TASK_SAVE_REQ;
            });

            // Save the Task
            cy.get('.gtx-entity-details-tab-content-header .gtx-save-button button')
                .click();
            cy.wait(ALIAS_TASK_TABLE_LOAD_REQ).then(() => {
                // task should now have new name in the table
                cy.get(ALIAS_TABLE)
                    .findTableRow(TASK_ID)
                    .should('contain.text', CHANGE_TASK_NAME);
            });
        });
    });
});