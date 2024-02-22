/* eslint-disable @typescript-eslint/naming-convention */
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
    File,
    Folder,
    FolderItemType,
    FolderListOptions,
    Image,
    InheritableItem,
    ItemPermissions,
    ItemType,
    LocalizationsResponse,
    Node,
    Normalized,
    Page,
    PageListOptions,
    Raw,
    ResponseCode,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { NEVER, Observable, of } from 'rxjs';
import { MultiDeleteModal, MultiDeleteResult } from '../../../shared/components/multi-delete-modal/multi-delete-modal.component';
import { ApplicationStateService, FeaturesActionsService, FolderActionsService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { Api } from '../api/api.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { I18nService } from '../i18n/i18n.service';
import { LocalizationsService } from '../localizations/localizations.service';
import { DecisionModalsService } from './decision-modals.service';

describe('DecisionModalsService', () => {

    const PAGE = 1234;
    const OTHERPAGE = 4321;
    const CURRENTNODE = 111;
    const MASTERNODE = 222;
    const OTHERNODE = 9876;
    const FOLDER = 333;
    const OTHERFOLDER = 444;

    let decisionModalsService: DecisionModalsService;
    let state: TestApplicationState;
    let entityResolver: MockEntityResolver;
    let modalService: MockModalService;
    let i18n: MockI18nService;
    let api: MockApi;
    let localizationsService: LocalizationsService;
    let folderActions: MockFolderActions;
    let featuresActions: MockFeaturesActions;
    let permissionService: MockPermissionService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        state = TestBed.get(ApplicationStateService);

        api = new MockApi();
        entityResolver = new MockEntityResolver(state);
        localizationsService = new LocalizationsService(
            api as any as Api,
            entityResolver as any as EntityResolver,
        );

        decisionModalsService = new DecisionModalsService(
            state,
            entityResolver as any as EntityResolver,
            (modalService = new MockModalService()) as any as ModalService,
            (i18n = new MockI18nService()) as any as I18nService,
            localizationsService,
            (folderActions = new MockFolderActions()) as any as FolderActionsService,
            (permissionService = new MockPermissionService()) as any,
            (featuresActions = new MockFeaturesActions()) as any as FeaturesActionsService,
        );

        state.mockState({
            entities: {
                node: {
                    [CURRENTNODE]: {
                        name: 'Node',
                        id: CURRENTNODE,
                    },
                    [MASTERNODE]: {
                        name: 'Master node',
                        id: MASTERNODE,
                    },
                },
            },
            folder: {
                activeFolder: FOLDER,
            },
        });
    });

    describe('selectPagesToPublish()', () => {

        it('show modal more than one language selected', fakeAsync(() => {
            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                .and.returnValue(Promise.resolve({ open(): void {} }));

            const page = {
                id: PAGE,
                type: 'page',
                language: 'en',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
                contentSetId: PAGE,
            } as Partial<Page<Normalized>> as Page<Normalized>;
            const otherPage = {
                id: OTHERPAGE,
                type: 'page',
                language: 'de',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                        [otherPage.id]: otherPage,
                    },
                },
            });

            decisionModalsService.selectPagesToPublish([page, otherPage]);
            tick();

            expect(modalService.fromComponent).toHaveBeenCalled();
        }));

        it('returns the selected pages of selected language variants', fakeAsync(() => {
            const page = {
                id: PAGE,
                type: 'page',
                language: 'en',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;
            const otherPage = {
                id: OTHERPAGE,
                type: 'page',
                language: 'de',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                .and.returnValue(
                    Promise.resolve({
                        open: (): Promise<Page[]> => Promise.resolve([page, otherPage]),
                    }),
                );

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                        [otherPage.id]: otherPage,
                    },
                },
            });

            let returnedValues: Page[];
            decisionModalsService.selectPagesToPublish([page as any, otherPage as any])
                .then(values => returnedValues = values);
            tick();

            expect(returnedValues).toEqual([page, otherPage]);
        }));

        describe('modal not required', () => {

            function testModalNotRequiredWithPage(page: any): void {
                modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                    .and.returnValue(Promise.resolve({ open: (): void => {} }));

                let returnedValues: Page[];
                decisionModalsService.selectPagesToPublish([page ])
                    .then(values => returnedValues = values);
                tick();

                expect(modalService.fromComponent).not.toHaveBeenCalled();
                expect(returnedValues).toEqual([page]);
            }

            it('does not ask the user for pages with only one language variant', fakeAsync(() => {
                testModalNotRequiredWithPage({
                    id: PAGE,
                    languageVariants: {
                        1: PAGE,
                    },
                });
            }));

            it('does not ask the user for pages with no language variants', fakeAsync(() => {
                testModalNotRequiredWithPage({
                    id: PAGE,
                    languageVariants: {},
                });
            }));

        });

    });

    describe('selectPagesToTakeOffline()', () => {

        it('shows a modal if the page has language variants', fakeAsync(() => {
            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                .and.returnValue(Promise.resolve({ open(): void {} }));

            const page = {
                id: PAGE,
                type: 'page',
                language: 'en',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
                contentSetId: PAGE,
            } as Partial<Page<Normalized>> as Page<Normalized>;
            const otherPage = {
                id: OTHERPAGE,
                type: 'page',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                        [otherPage.id]: otherPage,
                    },
                },
            });

            decisionModalsService.selectPagesToTakeOffline([page]);
            tick();

            expect(modalService.fromComponent).toHaveBeenCalled();
        }));

        it('returns the page IDs of the selected language variants', fakeAsync(() => {
            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                .and.returnValue(
                    Promise.resolve({
                        open: (): Promise<number[]> => Promise.resolve([PAGE, OTHERPAGE]),
                    }),
                );

            const page = {
                id: PAGE,
                type: 'page',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;
            const otherPage = {
                id: OTHERPAGE,
                type: 'page',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                        [otherPage.id]: otherPage,
                    },
                },
            });

            let returnedValues: number[];
            decisionModalsService.selectPagesToTakeOffline([page as any])
                .then(values => returnedValues = values);
            tick();

            expect(returnedValues).toEqual([PAGE, OTHERPAGE]);
        }));

        describe('modal not required', () => {

            function testModalNotRequiredWithPage(page: any): void {
                modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                    .and.returnValue(Promise.resolve({ open: (): void => {} }));

                let returnedValues: number[];
                decisionModalsService.selectPagesToTakeOffline([page ])
                    .then(values => returnedValues = values);
                tick();

                expect(modalService.fromComponent).not.toHaveBeenCalled();
                expect(returnedValues).toEqual([PAGE]);
            }

            it('does not ask the user for pages with only one language variant', fakeAsync(() => {
                testModalNotRequiredWithPage({
                    id: PAGE,
                    languageVariants: {
                        1: PAGE,
                    },
                });
            }));

            it('does not ask the user for pages with no language variants', fakeAsync(() => {
                testModalNotRequiredWithPage({
                    id: PAGE,
                    languageVariants: {},
                });
            }));

        });

    });

    describe('selectItemsToDelete()', () => {

        beforeEach(() => {
            // Item which is not localized in other channels
            api.folders.getLocalizations = (type, itemId) => of(<LocalizationsResponse> {
                masterId: itemId,
                masterNodeId: MASTERNODE,
                nodeIds: {
                    [itemId]: MASTERNODE,
                },
            });

        });

        it('requests a list of localized items in derived channels', fakeAsync(() => {
            api.folders.getLocalizations = jasmine.createSpy('FolderApi.getLocalizations')
                .and.returnValue(NEVER);
            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                .and.returnValue(Promise.resolve({ open(): void {} }));

            const firstPage = {
                id: PAGE,
                type: 'page',
                languageVariants: {
                    1: PAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            const secondPage = {
                id: OTHERPAGE,
                type: 'page',
                languageVariants: {
                    1: OTHERPAGE,
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            state.mockState({
                entities: {
                    page: {
                        [firstPage.id]: firstPage,
                        [secondPage.id]: secondPage,
                    },
                },
            });

            decisionModalsService.selectItemsToDelete([firstPage, secondPage]);
            tick();

            expect(api.folders.getLocalizations).toHaveBeenCalledWith('page', PAGE);
            expect(api.folders.getLocalizations).toHaveBeenCalledWith('page', OTHERPAGE);
        }));

        it('shows a modal if a page with language variants should be deleted', fakeAsync(() => {
            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                .and.returnValue(Promise.resolve({ open(): void {} }));

            const page = {
                id: PAGE,
                type: 'page',
                language: 'en',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
                contentSetId: PAGE,
            } as Partial<Page<Normalized>> as Page<Normalized>;

            const pageInOtherLanguage = {
                id: OTHERPAGE,
                type: 'page',
                language: 'de',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
                contentSetId: PAGE,
            } as Partial<Page<Normalized>> as Page<Normalized>;

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                        [pageInOtherLanguage.id]: pageInOtherLanguage,
                    },
                    node: {
                        [MASTERNODE]: {
                            id: MASTERNODE,
                            type: 'node',
                            name: 'Master Node',
                        },
                    },
                },
            });

            decisionModalsService.selectItemsToDelete([page]);
            tick();

            expect(modalService.fromComponent).toHaveBeenCalled();
            const args = (modalService.fromComponent as jasmine.Spy).calls.mostRecent().args;
            const expected: Partial<MultiDeleteModal> = {
                localizedItems: [],
                inheritedItems: [],
                otherItems: [page],
                pageLanguageVariants: {
                    [PAGE]: [page, pageInOtherLanguage],
                },
                formLanguageVariants: {},
                itemLocalizations: {
                    [PAGE]: [],
                },
            };

            expect(args[0]).toBe(MultiDeleteModal, 'wrong modal class');
            expect(args[2]).toEqual(expected, 'wrong modal parameters');
        }));

        it('returns the selected items to delete/unlocalize', fakeAsync(() => {
            const page = {
                id: PAGE,
                type: 'page',
                language: 'en',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
                contentSetId: PAGE,
            } as Partial<Page<Normalized>> as Page<Normalized>;

            const pageInOtherLanguage = {
                id: OTHERPAGE,
                type: 'page',
                language: 'de',
                languageVariants: {
                    1: PAGE,
                    2: OTHERPAGE,
                },
                contentSetId: PAGE,
            } as Partial<Page<Normalized>> as Page<Normalized>;

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                        [pageInOtherLanguage.id]: pageInOtherLanguage,
                    },
                    node: {
                        [MASTERNODE]: {
                            id: MASTERNODE,
                            type: 'node',
                            name: 'Master Node',
                        },
                    },
                },
            });

            modalService.mockResult(<MultiDeleteResult> {
                delete: [page, pageInOtherLanguage],
                deleteForms: {},
                unlocalize: [],
                localizations: {},
            });

            let returnedValues: MultiDeleteResult;
            decisionModalsService.selectItemsToDelete([page as any])
                .then(values => returnedValues = values);
            tick();

            expect(returnedValues).toEqual({
                delete: [page, pageInOtherLanguage],
                deleteForms: {},
                unlocalize: [],
                localizations: {},
            });
        }));

        it('works in nodes which have no languages configured', fakeAsync(() => {
            const page = {
                id: PAGE,
                type: 'page',
                languageVariants: {
                    // no languages configured!
                },
            } as Partial<Page<Normalized>> as Page<Normalized>;

            // Return data as if the page was localized in a channel
            api.folders.getLocalizations = jasmine.createSpy('FolderApi.getLocalizations')
                .and.returnValue(of({
                    masterId: MASTERNODE,
                    masterNodeId: MASTERNODE,
                    nodeIds: {
                        [page.id]: MASTERNODE,
                        [OTHERPAGE]: OTHERNODE,
                    },
                    responseInfo: {
                        responseCode: ResponseCode.OK,
                        responseMessage: '',
                    },
                } as any as LocalizationsResponse));

            modalService.mockResult(<MultiDeleteResult> {
                delete: [],
                deleteForms: {},
                unlocalize: [page],
                localizations: {},
            });

            state.mockState({
                entities: {
                    page: {
                        [page.id]: page,
                    },
                    node: {
                        [MASTERNODE]: {
                            id: MASTERNODE,
                            type: 'node',
                            name: 'Master Node',
                        },
                        [OTHERNODE]: {
                            id: OTHERNODE,
                            type: 'node',
                            name: 'Other Node',
                        },
                    },
                },
            });


            let returnedValues: MultiDeleteResult;
            decisionModalsService.selectItemsToDelete([page])
                .then(values => returnedValues = values);
            tick();

            expect(api.folders.getLocalizations).toHaveBeenCalledWith('page', page.id);

            const args = (modalService.fromComponent as jasmine.Spy).calls.mostRecent().args;
            const expected: Partial<MultiDeleteModal> = {
                localizedItems: [],
                inheritedItems: [],
                otherItems: [page],
                pageLanguageVariants: {
                    [PAGE]: [page],
                },
                formLanguageVariants: {},
                itemLocalizations: {
                    [PAGE]: [
                        { itemId: OTHERPAGE, nodeName: 'Other Node' },
                    ],
                },
            };

            expect(args[0]).toBe(MultiDeleteModal, 'wrong modal class');
            expect(args[2]).toEqual(expected, 'wrong modal parameters');

            expect(returnedValues).toEqual({
                delete: [],
                deleteForms: {},
                unlocalize: [page],
                localizations: {},
            });
        }));

        it('does display confirmation modal for pages with only one language variant if the user has wastebin permissions',
            fakeAsync(() => {
                permissionService.wastebin$ = of(true);

                modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                    .and.returnValue(Promise.resolve({ open: (): void => {} }));

                const page = {
                    id: PAGE,
                    languageVariants: {
                        1: PAGE,
                    },
                };

                let returnedValues: MultiDeleteResult;
                decisionModalsService.selectItemsToDelete([page as any])
                    .then(values => returnedValues = values);
                tick();

                expect(modalService.fromComponent).toHaveBeenCalled();
            }));

        it('does display confirmation modal for pages with only one language variant if the user does not have wastebin permissions',
            fakeAsync(() => {
                permissionService.wastebin$ = of(false);

                modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent')
                    .and.returnValue(Promise.resolve({ open: (): void => { } }));

                const page = {
                    id: PAGE,
                    languageVariants: {
                        1: PAGE,
                    },
                };

                decisionModalsService.selectItemsToDelete([page as any]);
                tick();

                expect(modalService.fromComponent).toHaveBeenCalled();
            }));

    });

    describe('showInheritedDialog()', () => {

        let inheritedItem: InheritableItem;
        let localItem: InheritableItem;
        let eventualAction: jasmine.Spy;

        beforeEach(() => {
            inheritedItem = {
                folderId: FOLDER,
                id: PAGE,
                inherited: true,
                inheritedFrom: 'Master node',
                inheritedFromId: MASTERNODE,
                type: 'page',
            } as any;

            localItem = {
                folderId: FOLDER,
                id: PAGE,
                inherited: false,
                inheritedFrom: 'Node',
                inheritedFromId: CURRENTNODE,
                type: 'page',
            } as any;

            eventualAction = jasmine.createSpy('eventualAction');
            folderActions.localizeItem = jasmine.createSpy('folderActions.localizeItem')
                .and.returnValue(Promise.resolve(localItem));
            folderActions.getItems = jasmine.createSpy('folderActions.getItems')
                .and.returnValue(Promise.resolve());
            featuresActions.checkFeature = jasmine.createSpy('featuresActions.checkFeature')
                .and.returnValue(Promise.resolve(false));
        });

        it('asks the user in which node to edit the item if it is inherited', fakeAsync(() => {
            modalService.dialog = jasmine.createSpy('ModalService.dialog')
                .and.returnValue(Promise.resolve({
                    open: (): Promise<any> => new Promise(() => {}),
                }));

            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();
            expect(modalService.dialog).toHaveBeenCalled();
        }));

        it('does not ask the user if the item is not inherited', fakeAsync(() => {
            modalService.dialog = jasmine.createSpy('ModalService.dialog');

            decisionModalsService.showInheritedDialog(localItem, CURRENTNODE).then(eventualAction);
            tick();
            expect(modalService.dialog).not.toHaveBeenCalled();
        }));

        it('does not ask the user if the always_localize setting is true', fakeAsync(() => {
            featuresActions.checkFeature = () => Promise.resolve(true);
            modalService.dialog = jasmine.createSpy('ModalService.dialog');

            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();
            expect(modalService.dialog).not.toHaveBeenCalled();
        }));

        it('edits the master item if the user chooses that', fakeAsync(() => {
            modalService.mockResult('editOriginal');

            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();

            expect(eventualAction).toHaveBeenCalledWith({ item: jasmine.anything(), nodeId: MASTERNODE });
        }));

        it('localizes the item if the user chooses that', fakeAsync(() => {
            modalService.mockResult('localize');
            folderActions.getItems = jasmine.createSpy('FolderActionsService.getItems');
            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();

            expect(folderActions.localizeItem).toHaveBeenCalledWith('page', PAGE, CURRENTNODE);
            expect(eventualAction).toHaveBeenCalledWith({ item: localItem, nodeId: CURRENTNODE });
        }));

        it('localizes the item if the user does not have permission to edit original', fakeAsync(() => {
            permissionService.forItem = jasmine.createSpy('PermissionService.forItem').and.returnValue(of({
                edit: false,
            } as ItemPermissions));
            folderActions.getItems = jasmine.createSpy('FolderActionsService.getItems');
            modalService.dialog = jasmine.createSpy('ModalService.dialog');
            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();

            expect(modalService.dialog).not.toHaveBeenCalled();
            expect(folderActions.localizeItem).toHaveBeenCalledWith('page', PAGE, CURRENTNODE);
            expect(eventualAction).toHaveBeenCalledWith({ item: localItem, nodeId: CURRENTNODE });
        }));

        it('refreshes the item list if an item is localized into the current folder', fakeAsync(() => {
            state.mockState({ folder: { ...state.now, activeFolder: FOLDER } });
            folderActions.getItems = jasmine.createSpy('FolderActionsService.getItems');

            modalService.mockResult('localize');
            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();

            expect(folderActions.getItems).toHaveBeenCalledWith(FOLDER, 'page');
        }));

        it('does not refresh the item list if an item is localized into a different folder', fakeAsync(() => {
            state.mockState({ folder: { ...state.now, activeFolder: OTHERFOLDER } });
            folderActions.getItems = jasmine.createSpy('FolderActionsService.getItems');

            modalService.mockResult('localize');
            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();

            expect(folderActions.getItems).not.toHaveBeenCalled();
        }));

        it('does not localize an item and resolves to undefined if the user cancels the modal', fakeAsync(() => {
            folderActions.getItems = jasmine.createSpy('folderActions.getItems');

            modalService.mockResult('');
            decisionModalsService.showInheritedDialog(inheritedItem, CURRENTNODE).then(eventualAction);
            tick();

            expect(eventualAction).toHaveBeenCalledWith(undefined);
            expect(folderActions.localizeItem).not.toHaveBeenCalled();
            expect(folderActions.getItems).not.toHaveBeenCalled();
        }));

    });

    describe('showTranslatePageDialog()', () => {

        let nonInheritedPage: Page;
        let inheritedPage: Page;

        beforeEach(() => {
            nonInheritedPage = {
                id: PAGE,
                master: true,
                masterNode: 'Node',
                masterNodeId: CURRENTNODE,
            } as any;

            inheritedPage = {
                id: PAGE,
                master: false,
                masterNode: 'Master node',
                masterNodeId: MASTERNODE,
                languageVariants: {
                    1: PAGE,
                },
                type: 'page',
            } as any;
        });

        it('does not show a modal if the passed page is not inherited', () => {
            modalService.dialog = jasmine.createSpy('ModalService.dialog');
            modalService.fromComponent = jasmine.createSpy('ModalService.fromComponent');

            decisionModalsService.showTranslatePageDialog(nonInheritedPage, CURRENTNODE);

            expect(modalService.dialog).not.toHaveBeenCalled();
            expect(modalService.fromComponent).not.toHaveBeenCalled();
        });

        it('shows a modal if the passed page is not the master', () => {
            modalService.dialog = jasmine.createSpy('ModalService.dialog')
                .and.returnValue(Promise.resolve({
                    open: (): Promise<any> => new Promise(() => {}),
                }));

            decisionModalsService.showTranslatePageDialog(inheritedPage, CURRENTNODE);

            expect(modalService.dialog).toHaveBeenCalled();
        });

        it('resolves the returned promise to the master node id if the user chooses the master node', fakeAsync(() => {
            modalService.mockResult(MASTERNODE);

            let result: number;
            decisionModalsService.showTranslatePageDialog(inheritedPage, CURRENTNODE)
                .then(val => result = val);
            tick();

            expect(modalService.dialog).toHaveBeenCalled();
            expect(result).toBe(MASTERNODE);
        }));

        it('resolves the returned promise to the local node id if the user chooses the local node', fakeAsync(() => {
            modalService.mockResult(CURRENTNODE);

            let result: number;
            decisionModalsService.showTranslatePageDialog(inheritedPage, CURRENTNODE)
                .then(val => result = val);
            tick();

            expect(modalService.dialog).toHaveBeenCalled();
            expect(result).toBe(CURRENTNODE);
        }));

        it('does not resolve the returned promise if the user cancels', fakeAsync(() => {
            modalService.dialog = jasmine.createSpy('ModalService.dialog')
                .and.returnValue(Promise.resolve({
                    open: (): Promise<any> => new Promise(() => {}),
                }));

            let resolved = false;
            decisionModalsService.showTranslatePageDialog(inheritedPage, CURRENTNODE)
                .then(() => resolved = true);
            tick();

            expect(modalService.dialog).toHaveBeenCalled();
            expect(resolved).toBe(false);
        }));

    });

});

class MockModalService {
    dialog(): void {
        throw new Error('dialog called but not mocked');
    }
    fromComponent(): void {
        throw new Error('fromComponent called but not mocked');
    }
    mockResult(result: any): void {
        this.dialog = jasmine.createSpy('ModalService.dialog')
            .and.callFake(() => Promise.resolve({
                open: (): Promise<any> => Promise.resolve(result),
            }));
        this.fromComponent = jasmine.createSpy('ModalService.fromComponent')
            .and.callFake(() => Promise.resolve({
                open: (): Promise<any> => Promise.resolve(result),
            }));
    }
}

class MockPermissionService {
    wastebin$: Observable<boolean> = of(true);
    forItem(): Observable<ItemPermissions> {
        return of({
            edit: true,
        } as ItemPermissions);
    }
}

class MockEntityResolver {
    constructor(private appState: ApplicationStateService) { }
    getNode(id: number): Node {
        return this.appState.now.entities.node[id];
    }
    getPage(id: number): Page {
        return this.appState.now.entities.page[id];
    }
}

class MockI18nService {
    translate(key: string, params?: any): string {
        return key;
    }
}

class MockApi {
    folders = {
        getLocalizations(type: ItemType, id: number): Observable<LocalizationsResponse> {
            throw new Error('getLocalizations called but not mocked');
        },
    };
}

class MockFolderActions implements Partial<FolderActionsService> {
    localizeItem(type: 'folder', itemId: number, channelId: number): Promise<Folder<Raw>>;
    localizeItem(type: 'page', itemId: number, channelId: number): Promise<Page<Raw>>;
    localizeItem(type: 'file', itemId: number, channelId: number): Promise<File<Raw>>;
    localizeItem(type: 'image', itemId: number, channelId: number): Promise<Image<Raw>>;
    localizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<InheritableItem<Raw>>;
    localizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<InheritableItem<Raw> | void>{
        throw new Error('localizeItem called but not mocked');
    }
    getItems(parentId: number, type: 'page', fetchAll?: boolean, options?: PageListOptions): Promise<void>;
    getItems(parentId: number, type: FolderItemType, fetchAll?: boolean, options?: FolderListOptions): Promise<void>;
    getItems(parentId: number, type: FolderItemType, fetchAll?: boolean, options: any = {}): Promise<void> {
        throw new Error('getItems called but not mocked');
    }
}

class MockFeaturesActions {
    checkFeature(feature: string): Promise<any> {
        throw new Error('checkFeature called but not mocked');
    }
}
