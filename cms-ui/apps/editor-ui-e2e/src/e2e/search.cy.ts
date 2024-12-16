/* eslint-disable @typescript-eslint/naming-convention */
import { ElasticSearchQuery, ElasticSearchQueryResponse, Feature, InheritableItem, Page, Variant } from '@gentics/cms-models';
import { EntityImporter, isVariant, minimalNode, skipableSuite, TestSize } from '@gentics/e2e-utils';
import '@gentics/e2e-utils/commands';
import { AUTH_ADMIN } from '../support/common';

describe('Search', () => {

    const IMPORTER = new EntityImporter();
    const ALIAS_SEARCH_BAR = '@searchBar';
    const ALIAS_SEARCH_INPUT = '@searchInput';
    const ALIAS_SEARCH_SUBMIT = '@searchSubmit';

    before(() => {
        cy.muteXHR();
        cy.wrap(null, { log: false })
            .then(() => cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 }));
    });

    beforeEach(() => {
        cy.muteXHR();
        cy.wrap(null, { log: false })
            .then(() => cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 }))
            .then(() => cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL), { log: false, timeout: 60_000 }));
    });

    describe('via Gentics CMS', () => {
        before(() => {
            // Disable Elasticsearch for all tests
            cy.wrap(null, { log: false })
                .then(() => cy.wrap(IMPORTER.cleanupTest(), { timeout: 60_000, log: false }))
                .then(() => cy.wrap(IMPORTER.setupFeatures({
                    [Feature.ELASTICSEARCH]: false,
                }), { timeout: 60_000, log: false }));
        });

        beforeEach(() => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);

            cy.get('gtx-top-bar .search-container chip-search-bar')
                .as(ALIAS_SEARCH_BAR)
                .find('input.gtx-chipsearchbar-value')
                .as(ALIAS_SEARCH_INPUT)
                .parents('chip-search-bar')
                .find('.gtx-chipsearchbar-button-container .gtx-chipsearchbar-button gtx-button[data-action="search"]')
                .as(ALIAS_SEARCH_SUBMIT);
        });

        it('should search for the requested term with search button', () => {
            const SEARCH_TERM = 'test';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept({
                pathname: '/rest/folder/getPages/*',
                query: {
                    search: SEARCH_TERM,
                },
            }, req => {
                req.alias = ALIAS_PAGE_SEARCH;
            });

            cy.get(ALIAS_SEARCH_INPUT)
                .type(SEARCH_TERM);
            cy.get(ALIAS_SEARCH_SUBMIT)
                .click();

            cy.wait(ALIAS_PAGE_SEARCH);
        });

        it('should search for the requested term with enter press', () => {
            const SEARCH_TERM = 'test';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept({
                pathname: '/rest/folder/getPages/*',
                query: {
                    search: SEARCH_TERM,
                },
            }, req => {
                req.alias = ALIAS_PAGE_SEARCH;
            });

            cy.get(ALIAS_SEARCH_INPUT)
                .type(`${SEARCH_TERM}{enter}`);

            cy.wait(ALIAS_PAGE_SEARCH);
        });

        it('should search a string chip as expected', () => {
            const CHIP_NAME = 'filename';
            const CHIP_VALUE = 'one';
            const CHIP_OPERTATOR = '';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept({
                pathname: '/rest/folder/getPages/*',
                query: {
                    [CHIP_NAME]: `*${CHIP_VALUE}*`,
                },
            }, req => {
                req.alias = ALIAS_PAGE_SEARCH;
            });

            // Hacky workaround for the annoting `autofocus` directive.
            // It might steal the focus from the search-chip input and write it into the general search bar
            // instead, which will break the test.
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(5_000);
            cy.get(ALIAS_SEARCH_BAR)
                .addSearchChip({
                    property: CHIP_NAME,
                    operator: CHIP_OPERTATOR,
                    value: CHIP_VALUE,
                })
                .search();

            cy.wait(ALIAS_PAGE_SEARCH);
        });

        /*
         * SUP-17701: Dates were incorrectly sent to the backend.
         */
        it('should search a date chip as expected', () => {
            const CHIP_NAME = 'created';
            const CHIP_VALUE = new Date();
            CHIP_VALUE.setFullYear(CHIP_VALUE.getFullYear() - 1, CHIP_VALUE.getMonth() - 1, CHIP_VALUE.getDate() - 2);
            const TIME = Math.floor(CHIP_VALUE.getTime() / 1000);
            const CHIP_OPERTATOR = 'AFTER';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept({
                pathname: '/rest/folder/getPages/*',
            }, req => {
                const timestamp = parseInt(req.query[`${CHIP_NAME}since`] as any, 10);
                if (Number.isInteger(timestamp)) {
                    let diff = TIME - timestamp;
                    if (diff < 0) {
                        diff *= 1;
                    }
                    // Check for leeway
                    if (diff <= 1000) {
                        req.alias = ALIAS_PAGE_SEARCH;
                    }
                }
            });

            // Hacky workaround for the annoting `autofocus` directive.
            // It might steal the focus from the search-chip input and write it into the general search bar
            // instead, which will break the test.
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(5_000);
            cy.get(ALIAS_SEARCH_BAR)
                .addSearchChip({
                    property: CHIP_NAME,
                    operator: CHIP_OPERTATOR,
                    value: CHIP_VALUE,
                })
                .search();

            cy.wait(ALIAS_PAGE_SEARCH);
        });
    });

    skipableSuite(isVariant(Variant.ENTERPRISE), 'via ElasticSearch', () => {
        before(() => {
            // Enable Elasticsearch for all tests
            cy.wrap(null, { log: false })
                .then(() => cy.wrap(IMPORTER.cleanupTest(), { timeout: 60_000, log: false }))
                .then(() => cy.wrap(IMPORTER.setupFeatures({
                    [Feature.ELASTICSEARCH]: true,
                }), { timeout: 60_000, log: false }));
        });

        beforeEach(() => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.selectNode(IMPORTER.get(minimalNode)!.id);

            cy.get('gtx-top-bar .search-container chip-search-bar')
                .as(ALIAS_SEARCH_BAR)
                .find('input.gtx-chipsearchbar-value')
                .as(ALIAS_SEARCH_INPUT)
                .parents('chip-search-bar')
                .find('.gtx-chipsearchbar-button-container .gtx-chipsearchbar-button gtx-button[data-action="search"]')
                .as(ALIAS_SEARCH_SUBMIT);
        });

        it('should search for the requested term with search button', () => {
            const SEARCH_TERM = 'test';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept<ElasticSearchQuery, ElasticSearchQueryResponse<InheritableItem>>({
                pathname: '/rest/elastic/page/_search',
            }, req => {
                const validBody = Cypress._.isEqual(req.body.query.bool, {
                    must: [
                        {
                            bool: {
                                should: [
                                    {
                                        multi_match: {
                                            fields: [
                                                'name^2',
                                                'path',
                                                'description',
                                                'content',
                                            ],
                                            query: SEARCH_TERM,
                                        },
                                    },
                                    {
                                        wildcard: {
                                            niceUrl: {
                                                value: `*${SEARCH_TERM}*`,
                                            },
                                        },
                                    },
                                    {
                                        wildcard: {
                                            filename: {
                                                value: `*${SEARCH_TERM}*`,
                                                boost: 2,
                                            },
                                        },
                                    },
                                    {
                                        wildcard: {
                                            'name.raw': {
                                                value: `*${SEARCH_TERM}*`,
                                                boost: 2,
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                });

                if (validBody) {
                    req.alias = ALIAS_PAGE_SEARCH;
                }
            });

            cy.get(ALIAS_SEARCH_INPUT)
                .type(SEARCH_TERM);
            cy.get(ALIAS_SEARCH_SUBMIT)
                .click();

            cy.wait(ALIAS_PAGE_SEARCH);
        });

        it('should search for the requested term with enter press', () => {
            const SEARCH_TERM = 'test';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept<ElasticSearchQuery, ElasticSearchQueryResponse<InheritableItem>>({
                pathname: '/rest/elastic/page/_search',
            }, req => {
                const validBody = Cypress._.isEqual(req.body.query.bool, {
                    must: [
                        {
                            bool: {
                                should: [
                                    {
                                        multi_match: {
                                            fields: [
                                                'name^2',
                                                'path',
                                                'description',
                                                'content',
                                            ],
                                            query: SEARCH_TERM,
                                        },
                                    },
                                    {
                                        wildcard: {
                                            niceUrl: {
                                                value: `*${SEARCH_TERM}*`,
                                            },
                                        },
                                    },
                                    {
                                        wildcard: {
                                            filename: {
                                                value: `*${SEARCH_TERM}*`,
                                                boost: 2,
                                            },
                                        },
                                    },
                                    {
                                        wildcard: {
                                            'name.raw': {
                                                value: `*${SEARCH_TERM}*`,
                                                boost: 2,
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                });

                if (validBody) {
                    req.alias = ALIAS_PAGE_SEARCH;
                }
            });

            cy.get(ALIAS_SEARCH_INPUT)
                .type(`${SEARCH_TERM}{enter}`);

            cy.wait(ALIAS_PAGE_SEARCH);
        });

        it('should search a string chip as expected', () => {
            const CHIP_NAME = 'filename';
            const CHIP_VALUE = 'one';
            const CHIP_OPERTATOR = '';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept<ElasticSearchQuery, ElasticSearchQueryResponse<InheritableItem>>({
                pathname: '/rest/elastic/page/_search',
            }, req => {
                const validBody = Cypress._.isEqual(req.body.query.bool, {
                    must: [
                        {
                            wildcard: {
                                [CHIP_NAME]: {
                                    value: `*${CHIP_VALUE}*`,
                                },
                            },
                        },
                    ],
                });

                if (validBody) {
                    req.alias = ALIAS_PAGE_SEARCH;
                }
            });

            // Hacky workaround for the annoting `autofocus` directive.
            // It might steal the focus from the search-chip input and write it into the general search bar
            // instead, which will break the test.
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(5_000);
            cy.get(ALIAS_SEARCH_BAR)
                .addSearchChip({
                    property: CHIP_NAME,
                    operator: CHIP_OPERTATOR,
                    value: CHIP_VALUE,
                })
                .search();

            cy.wait(ALIAS_PAGE_SEARCH);
        });

        /*
         * SUP-17701: Dates were incorrectly sent to the backend.
         */
        it('should search a date chip as expected', () => {
            const CHIP_NAME = 'created';
            const CHIP_VALUE = new Date();
            CHIP_VALUE.setFullYear(CHIP_VALUE.getFullYear() - 1, CHIP_VALUE.getMonth() - 1, CHIP_VALUE.getDate() - 2);
            const TIME = Math.floor(CHIP_VALUE.getTime() / 1000);
            const CHIP_OPERTATOR = 'AFTER';
            const ALIAS_PAGE_SEARCH = '@pageSearchReq';

            cy.intercept({
                pathname: '/rest/elastic/page/_search',
            }, req => {
                const validBody = Cypress._.isEqual(req.body.query.bool, {
                    must: [
                        {
                            range: {
                                [CHIP_NAME]: {
                                    format: 'yyyy-MM-dd',
                                    gte: CHIP_VALUE.toISOString().substring(0, 10),
                                },
                            },
                        },
                    ],
                });

                if (validBody) {
                    req.alias = ALIAS_PAGE_SEARCH;
                }
            });

            // Hacky workaround for the annoting `autofocus` directive.
            // It might steal the focus from the search-chip input and write it into the general search bar
            // instead, which will break the test.
            // eslint-disable-next-line cypress/no-unnecessary-waiting
            cy.wait(5_000);
            cy.get(ALIAS_SEARCH_BAR)
                .addSearchChip({
                    property: CHIP_NAME,
                    operator: CHIP_OPERTATOR,
                    value: CHIP_VALUE,
                })
                .search();

            cy.wait(ALIAS_PAGE_SEARCH);
        });
    });
});
