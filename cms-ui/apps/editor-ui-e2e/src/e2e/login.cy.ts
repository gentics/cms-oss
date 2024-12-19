import '@gentics/e2e-utils/commands';
import { Feature, LoginResponse, Variant } from '@gentics/cms-models';
import { EntityImporter, isVariant, skipableSuite, TestSize } from '@gentics/e2e-utils';
import { AUTH_ADMIN, AUTH_KEYCLOAK } from '../support/common';

describe('Login', () => {

    const IMPORTER = new EntityImporter();

    beforeEach(() => {
        cy.muteXHR();
    });

    describe('Without keycloak feature enabled', () => {
        // Make sure to have keycloak disabled for these tests
        before(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.setupFeatures({
                    [Feature.KEYCLOAK]: false,
                }), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });
        });

        beforeEach(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });
        });

        it('should be able to login', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.get('project-editor').should('exist');
        });

        it('should skip login if already logged in', () => {
            cy.navigateToApp();

            const ALIAS_REQ_VALIDATE = '@reqValidate';

            // Get the credentials from the auth fixture
            cy.fixture('auth.json').then(auth => {
                return cy.window().then(win => {
                    return cy.request<LoginResponse>({
                        method: 'POST',
                        url: '/rest/auth/login',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            login: auth[AUTH_ADMIN].username,
                            password: auth[AUTH_ADMIN].password,
                        }),
                    }).then(res => {
                        const sid = `${res.body.sid}`;
                        win.localStorage.setItem('GCMSUI_sid', sid);

                        cy.intercept({
                            method: 'GET',
                            pathname: '/rest/user/me',
                            query: {
                                sid,
                            },
                        }, req => {
                            req.alias = ALIAS_REQ_VALIDATE;
                        });

                        // Refresh and make the app verify the login in the background
                        cy.reload(true);

                        // Wait till validated
                        cy.wait(ALIAS_REQ_VALIDATE, { timeout: 60_000 });

                        // Should show the default view now
                        cy.get('project-editor').should('exist');
                    });
                });
            });
        });
    });

    // Keycloak is an enterprise edition feature, therefore we can only test it in that variant.
    skipableSuite(isVariant(Variant.ENTERPRISE), 'With keycloak feature enabled', () => {
        const ALIAS_SSO_LOGIN = '@ssoLogin';

        // Make sure to have keycloak disabled for these tests
        before(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.setupFeatures({
                    [Feature.KEYCLOAK]: true,
                }), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });
        });

        beforeEach(() => {
            cy.wrap(IMPORTER.clearClient(), { log: false }).then(() => {
                return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
            }).then(() => {
                return cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 });
            });
        });

        it('should be able to login (skip-sso)', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.get('project-editor').should('exist');
        });

        it.skip('should be able to login (default without skip-sso)', () => {
            cy.navigateToApp('', true);
            cy.login(AUTH_KEYCLOAK, true);

            // Wait for login to be finished
            cy.intercept({
                pathname: '/rest/auth/ssologin',
            }, req => {
                req.alias = ALIAS_SSO_LOGIN;
            });
            cy.wait(ALIAS_SSO_LOGIN, { timeout: 60_000 });

            cy.get('project-editor').should('exist');
        });
    });
});
