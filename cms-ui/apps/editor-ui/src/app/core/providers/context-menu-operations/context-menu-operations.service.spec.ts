/* eslint-disable @typescript-eslint/naming-convention */
import { Type } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { I18nNotificationService, TranslatedNotificationOptions } from '@gentics/cms-components';
import { EditMode, RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import {
    AllowedSelectionType,
    AllowedSelectionTypeMap,
    CmsFormData,
    File,
    Folder,
    FolderItemOrNodeSaveOptionsMap,
    FolderItemType,
    Form,
    Image,
    InheritableItem,
    ItemInNode,
    ItemType,
    ItemTypeMap,
    Page,
    Raw,
    TagInContainer,
    Template,
} from '@gentics/cms-models';
import {
    getExampleFolderData,
    getExampleFolderDataNormalized,
    getExampleNodeDataNormalized,
    getExampleTemplateData,
} from '@gentics/cms-models/testing/test-data.mock';
import { IDialogConfig, IModalDialog, IModalInstance, IModalOptions, ModalDialogComponent, ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { cloneDeep } from 'lodash-es';
import { Observable, of } from 'rxjs';
import { LinkTemplateModal, MultiDeleteResult } from '../../../shared/components';
import { RepositoryBrowserClient } from '../../../shared/providers';
import { LinkTemplateService } from '../../../shared/providers/link-template/link-template.service';
import { ApplicationStateService, FolderActionsService, PostUpdateBehavior, STATE_MODULES, TemplateActionsService, WastebinActionsService } from '../../../state';
import { ContentStagingActionsService, UsageActionsService } from '../../../state/index';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { ApiError } from '../api';
import { DecisionModalsService } from '../decision-modals/decision-modals.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { FavouritesService } from '../favourites/favourites.service';
import { InstructionActions, NavigationService } from '../navigation/navigation.service';
import { PermissionService } from '../permissions/permission.service';
import { ContextMenuOperationsService } from './context-menu-operations.service';

const ACTIVE_NODE_ID = 123;
const ITEM_ID = 5;
const LOCALIZED_ITEM_ID = 72;
const DIFFERENT_ITEM_ID = 6;

describe('ContextMenuOperationsService', () => {

    let contextMenuOperationsService: ContextMenuOperationsService;
    let decisionModalsService: MockDecisionModalsService;
    let errorHandler: MockErrorHandler;
    let folderActions: MockFolderActions;
    let modalService: MockModalService;
    let navigationService: MockNavigationService;
    let state: TestApplicationState;
    let wastebinActions: MockWastebinActions;
    let usageActions: MockUsageActions;
    let repositoryBrowserClient: MockRepositoryBrowserClientService;
    let templateActions: MockTemplateActions;

    let linkTemplate: LinkTemplateService;
    let entityResolver: EntityResolver;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                ContextMenuOperationsService,
                { provide: DecisionModalsService, useClass: MockDecisionModalsService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: Router, useClass: MockRouter },
                { provide: I18nNotificationService, useClass: MockI18nNotification },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: WastebinActionsService, useClass: MockWastebinActions },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: ModalService, useClass: MockModalService },
                { provide: UsageActionsService, useClass: MockUsageActions },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: FavouritesService, useClass: MockFavouritesService },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClientService },
                { provide: TemplateActionsService, useClass: MockTemplateActions },
                { provide: ContentStagingActionsService, useClass: MockContentStagingActions },
            ],
        });

        // Main services
        state = TestBed.inject(ApplicationStateService) as TestApplicationState;
        contextMenuOperationsService = TestBed.inject(ContextMenuOperationsService);

        // Mocked ones
        decisionModalsService = TestBed.inject(DecisionModalsService);
        errorHandler = TestBed.inject(ErrorHandler);
        folderActions = TestBed.inject(FolderActionsService) as any;
        modalService = TestBed.inject(ModalService);
        navigationService = TestBed.inject(NavigationService) as any;
        wastebinActions = TestBed.inject(WastebinActionsService);
        usageActions = TestBed.inject(UsageActionsService) as any;
        repositoryBrowserClient = TestBed.inject(RepositoryBrowserClient);
        templateActions = TestBed.inject(TemplateActionsService);
        entityResolver = TestBed.inject(EntityResolver);

        linkTemplate = new MockLinkTemplateService() as any as LinkTemplateService;
    });

    afterEach(() => {
        entityResolver.ngOnDestroy();
        contextMenuOperationsService.ngOnDestroy();
    })

    describe('localizing', () => {
        let activeNodeId: number;
        let item: any;
        let localItem: any;
        let navigateSpy: any;
        let detailSpy: any;
        let detailOrModalSpy: any;
        let modalSpy: any;

        beforeEach(() => {
            activeNodeId = ACTIVE_NODE_ID;
            item = { name: 'Some page', type: 'page', id: ITEM_ID };
            localItem = { name: 'Some localized page', type: 'page', id: LOCALIZED_ITEM_ID };

            folderActions.localizeItem = jasmine.createSpy('folderActions.localizeItem')
                .and.returnValue(Promise.resolve(localItem));
            folderActions.refreshList = jasmine.createSpy('folderActions.refreshList')
                .and.returnValue(Promise.resolve());

            navigateSpy = jasmine.createSpy('navigate');
            detailSpy = jasmine.createSpy('detail').and.returnValue({
                navigate: navigateSpy,
            });
            detailOrModalSpy = jasmine.createSpy('detailOrModal').and.callFake(((
                nodeId: number,
                itemType: FolderItemType | 'node' | 'channel',
                itemId: number,
                editMode: EditMode,
            ) => {
                return detailSpy(nodeId, itemType, itemId, editMode);
            }) as any);
            navigationService.detailOrModal = detailOrModalSpy;
            navigationService.detail = detailSpy;
        });

        it('calls `createPageVariations` function right', fakeAsync(() => {
            const items: any[] = [
                { name: 'Some page1', type: 'page', id: ITEM_ID + 1 },
                { name: 'Some page2', type: 'page', id: ITEM_ID + 2 },
                { name: 'Some page3', type: 'page', id: ITEM_ID + 3 },
            ];

            const targetFolders: Folder[] = [ getExampleFolderData() ];

            spyOn(repositoryBrowserClient, 'openRepositoryBrowser').and.returnValue(Promise.resolve(targetFolders));

            spyOn(folderActions, 'createPageVariations').and.returnValue(Promise.resolve());

            spyOn(usageActions, 'getTotalUsage').and.returnValue(null);

            contextMenuOperationsService.createVariationsClicked(items, activeNodeId);
            tick();

            expect(folderActions.createPageVariations).toHaveBeenCalledWith(items, activeNodeId, targetFolders);
            expect(usageActions.getTotalUsage).toHaveBeenCalled();
        }));

        it('item refreshes item list', fakeAsync(() => {
            contextMenuOperationsService.localize(item, activeNodeId);

            expect(folderActions.localizeItem).toHaveBeenCalledWith('page', ITEM_ID, ACTIVE_NODE_ID);
            tick();
            expect(folderActions.refreshList).toHaveBeenCalledWith('page');
        }));

        it('currently previewed item refreshes the content frame', fakeAsync(() => {
            state.mockState({ editor: { editorIsOpen: true, itemId: ITEM_ID } });

            contextMenuOperationsService.localize(item, activeNodeId);

            tick();
            expect(detailOrModalSpy).toHaveBeenCalledWith(ACTIVE_NODE_ID, 'page', LOCALIZED_ITEM_ID, 'preview');
            expect(detailSpy).toHaveBeenCalledWith(ACTIVE_NODE_ID, 'page', LOCALIZED_ITEM_ID, 'preview');
            expect(navigateSpy).toHaveBeenCalled();
        }));

        it('different item refreshes item list but not content-frame', fakeAsync(() => {
            item = { name: 'Some page', type: 'page', id: DIFFERENT_ITEM_ID };
            state.mockState({ editor: { editorIsOpen: true, itemId: ITEM_ID } });

            contextMenuOperationsService.localize(item, activeNodeId);

            expect(folderActions.localizeItem).toHaveBeenCalledWith('page', DIFFERENT_ITEM_ID, ACTIVE_NODE_ID);
            tick();
            expect(folderActions.refreshList).toHaveBeenCalledWith('page');

            expect(detailSpy).not.toHaveBeenCalled();
            expect(navigateSpy).not.toHaveBeenCalled();
        }));

        it('does not open item in the content frame if it is closed', fakeAsync(() => {
            state.mockState({ editor: { editorIsOpen: false } });

            contextMenuOperationsService.localize(item, activeNodeId);

            tick();
            expect(detailSpy).not.toHaveBeenCalled();
            expect(navigateSpy).not.toHaveBeenCalled();
        }));

        it('successfully opens a link template modal with correct parameters ', fakeAsync(() => {
            linkTemplate = new MockLinkTemplateService() as any as LinkTemplateService;
            const nodeId = 23;
            const folderId = 42;
            const node = getExampleNodeDataNormalized({ id: nodeId });
            const folder = getExampleFolderDataNormalized({ id: folderId });
            state.mockState({ entities: {
                node: {
                    [nodeId]: node,
                },
                folder: {
                    [folderId]: folder,
                },
            }});

            const modalResult: IModalInstance<LinkTemplateModal> = {
                // Additional parameters do not need to be mocked, as they aren't called, since this isn't tested interactively
                instance: new LinkTemplateModal(state, linkTemplate, folderActions as unknown as FolderActionsService, null, null),
                element: null,
                open: () => Promise.resolve(),
            };

            modalService.fromComponent = jasmine.createSpy('modalService.fromComponent')
                .and.returnValue(Promise.resolve(modalResult));

            contextMenuOperationsService.linkTemplatesToFolder(nodeId, folderId);

            expect(modalService.fromComponent).toHaveBeenCalledWith(LinkTemplateModal, { padding: true, width: '1000px' }, { nodeId, folderId } as any);
        }));

        it('successfully opens a link template modal with correct parameters with CMS feature folder_based_template_selection', fakeAsync(() => {
            state.mockState({
                features: {
                    folder_based_template_selection: true,
                },
            });

            const nodeId = 23;
            const folderId = 42;
            const templateId = 115;
            const recursive = true;
            const linkTemplateRequestSucceeded = true;

            // configure spies
            spyOn(repositoryBrowserClient, 'openRepositoryBrowser').and.returnValue(Promise.resolve([getExampleTemplateData()]));
            modalService.fromComponent = jasmine.createSpy('modalService.fromComponent')
                .and.returnValue(Promise.resolve([getExampleTemplateData()]));
            modalService.dialog = jasmine.createSpy('modalService.dialog')
                .and.returnValue(Promise.resolve({ open: () => Promise.resolve(recursive) }));
            templateActions.linkTemplatesToFolders = jasmine.createSpy('templateActions.linkTemplatesToFolders')
                .and.returnValue(of(linkTemplateRequestSucceeded));
            folderActions.getTemplates = jasmine.createSpy('folderActions.getTemplates');
            contextMenuOperationsService.linkTemplatesToFolder(nodeId, folderId);

            tick();

            // set expectations
            expect(repositoryBrowserClient.openRepositoryBrowser).toHaveBeenCalledWith({
                allowedSelection: ['template'],
                selectMultiple: true,
                startNode: nodeId,
                startFolder: folderId,
            });
            expect(templateActions.linkTemplatesToFolders).toHaveBeenCalledWith(nodeId, [templateId], [folderId], recursive);
            expect(folderActions.getTemplates).toHaveBeenCalled();
        }));

    });


    describe('deleteItems', () => {

        describe('form', () => {

            const basicFormData: CmsFormData = {
                email: 'email@email.com',
                successurl: 'https://successurl.com',
                mailsubject_i18n: {
                    en: 'Email Subject',
                    de: 'E-Mail-Betreff',
                },
                mailtemp_i18n: {
                    en: 'Email Template',
                    de: 'E-Mail-Vorlage',
                },
                elements: [{
                    globalId: '7d03c625-b95d-4bec-a85d-db00adf940d5',
                    name: 'buttons_7d03c625_b95d_4bec_a85d_db00adf940d5',
                    type: 'buttons',
                    active: true,
                    elements: [],
                    submitlabel_i18n: {
                        en: 'Label',
                        de: 'Beschriftung',
                    },
                    showreset_i18n: {},
                    resetlabel_i18n: {},
                }, {
                    globalId: '5ba859a2-cd08-45f2-8636-ed98bb144679',
                    name: 'formpage_5ba859a2_cd08_45f2_8636_ed98bb144679',
                    type: 'formpage',
                    active: true,
                    elements: [{
                        globalId: 'a2d562eb-be5c-4fdd-b5e9-b461261e852f',
                        name: 'selectgroup_a2d562eb_be5c_4fdd_b5e9_b461261e852f',
                        type: 'selectgroup',
                        active: true,
                        elements: [ ],
                        label_i18n: {
                            en: 'Label',
                            de: 'Beschriftung',
                        },
                        mandatory_i18n: {},
                        info_i18n: {},
                        select_type_i18n: {},
                        additional_element_i18n: {},
                        options: [{
                            key: 'Key',
                            value_i18n: {
                                en: 'Value',
                                de: 'Wert',
                            },
                        }, {
                            key: 'Key 2',
                            value_i18n: {
                                en: 'Value 2',
                                de: 'Wert 2',
                            },
                        }, {
                            key: 'Key 3',
                            value_i18n: {
                                en: 'Value 3',
                            },
                        }],
                    }],
                    description_i18n: {
                        en: 'Label',
                        de: 'Beschriftung',
                    },
                    info_i18n: {
                        en: 'Label',
                    },
                }],
            };

            const basicFormDataWithoutEn: CmsFormData = {
                email: 'email@email.com',
                successurl: 'https://successurl.com',
                mailsubject_i18n: {
                    de: 'E-Mail-Betreff',
                },
                mailtemp_i18n: {
                    de: 'E-Mail-Vorlage',
                },
                elements: [{
                    globalId: '7d03c625-b95d-4bec-a85d-db00adf940d5',
                    name: 'buttons_7d03c625_b95d_4bec_a85d_db00adf940d5',
                    type: 'buttons',
                    active: true,
                    elements: [],
                    submitlabel_i18n: {
                        de: 'Beschriftung',
                    },
                    showreset_i18n: {},
                    resetlabel_i18n: {},
                }, {
                    globalId: '5ba859a2-cd08-45f2-8636-ed98bb144679',
                    name: 'formpage_5ba859a2_cd08_45f2_8636_ed98bb144679',
                    type: 'formpage',
                    active: true,
                    elements: [{
                        globalId: 'a2d562eb-be5c-4fdd-b5e9-b461261e852f',
                        name: 'selectgroup_a2d562eb_be5c_4fdd_b5e9_b461261e852f',
                        type: 'selectgroup',
                        active: true,
                        elements: [ ],
                        label_i18n: {
                            de: 'Beschriftung',
                        },
                        mandatory_i18n: {},
                        info_i18n: {},
                        select_type_i18n: {},
                        additional_element_i18n: {},
                        options: [{
                            key: 'Key',
                            value_i18n: {
                                de: 'Wert',
                            },
                        }, {
                            key: 'Key 2',
                            value_i18n: {
                                de: 'Wert 2',
                            },
                        }, {
                            key: 'Key 3',
                            value_i18n: {
                            },
                        }],
                    }],
                    description_i18n: {
                        de: 'Beschriftung',
                    },
                    info_i18n: {
                    },
                }],
            };

            const basicFormDataWithoutDe: CmsFormData = {
                email: 'email@email.com',
                successurl: 'https://successurl.com',
                mailsubject_i18n: {
                    en: 'Email Subject',
                },
                mailtemp_i18n: {
                    en: 'Email Template',
                },
                elements: [{
                    globalId: '7d03c625-b95d-4bec-a85d-db00adf940d5',
                    name: 'buttons_7d03c625_b95d_4bec_a85d_db00adf940d5',
                    type: 'buttons',
                    active: true,
                    elements: [],
                    submitlabel_i18n: {
                        en: 'Label',
                    },
                    showreset_i18n: {},
                    resetlabel_i18n: {},
                }, {
                    globalId: '5ba859a2-cd08-45f2-8636-ed98bb144679',
                    name: 'formpage_5ba859a2_cd08_45f2_8636_ed98bb144679',
                    type: 'formpage',
                    active: true,
                    elements: [{
                        globalId: 'a2d562eb-be5c-4fdd-b5e9-b461261e852f',
                        name: 'selectgroup_a2d562eb_be5c_4fdd_b5e9_b461261e852f',
                        type: 'selectgroup',
                        active: true,
                        elements: [ ],
                        label_i18n: {
                            en: 'Label',
                        },
                        mandatory_i18n: {},
                        info_i18n: {},
                        select_type_i18n: {},
                        additional_element_i18n: {},
                        options: [{
                            key: 'Key',
                            value_i18n: {
                                en: 'Value',
                            },
                        }, {
                            key: 'Key 2',
                            value_i18n: {
                                en: 'Value 2',
                            },
                        }, {
                            key: 'Key 3',
                            value_i18n: {
                                en: 'Value 3',
                            },
                        }],
                    }],
                    description_i18n: {
                        en: 'Label',
                    },
                    info_i18n: {
                        en: 'Label',
                    },
                }],
            };

            let wastebinActionsMoveItemsToWastebinSpy: jasmine.Spy;
            let folderActionsUpdateItemSpy: jasmine.Spy;

            beforeEach(() => {
                wastebinActionsMoveItemsToWastebinSpy = spyOn(wastebinActions, 'moveItemsToWastebin')
                    .and.callFake((<T extends ItemType>(_type: 'folder' | 'page' | 'file' | 'form' | 'image', ids: number[]) => {
                        return Promise.resolve({ succeeded: ids ? ids.length : 0, failed: 0, error: undefined });
                    }) as any);

                folderActionsUpdateItemSpy = spyOn(folderActions, 'updateItem')
                    .and.callFake((<T extends ItemType>(_type: T, _itemId: number, payload: Partial<ItemTypeMap<Raw>[T]>) => {
                        return Promise.resolve(payload);
                    }) as any);

                spyOn(folderActions, 'refreshList').and.returnValue(undefined);
            });

            it('will not trigger any deletions or updates, if no forms are selected', async () => {
                const type: FolderItemType = 'form';
                const items: Form[] = [
                    { name: 'Some Form 1', type: 'form', id: ITEM_ID + 1, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                    { name: 'Some Form 2', type: 'form', id: ITEM_ID + 2, languages: ['en'], data: cloneDeep(basicFormDataWithoutDe) } as Form,
                    { name: 'Some Form 3', type: 'form', id: ITEM_ID + 3, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                ];
                const activeNodeId: number = ACTIVE_NODE_ID;

                spyOn(decisionModalsService, 'selectItemsToDelete').and.returnValue(Promise.resolve({
                    delete: items,
                    deleteForms: { [ITEM_ID + 1]: [], [ITEM_ID + 2]: [], [ITEM_ID + 3]: [] },
                    localizations: [],
                    unlocalize: [],
                }));

                const expectedResult: number[] = [];
                const result = await contextMenuOperationsService.deleteItems(type, items, activeNodeId);

                expect(wastebinActionsMoveItemsToWastebinSpy).not.toHaveBeenCalled();
                expect(folderActionsUpdateItemSpy).not.toHaveBeenCalled();
                expect(result).toEqual(expectedResult);
            });

            it('will only trigger updates, if not all languages of a form are deleted', async () => {
                const type: FolderItemType = 'form';
                const items: Form[] = [
                    { name: 'Some Form 1', type: 'form', id: ITEM_ID + 1, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                    { name: 'Some Form 2', type: 'form', id: ITEM_ID + 2, languages: ['en'], data: cloneDeep(basicFormDataWithoutDe) } as Form,
                    { name: 'Some Form 3', type: 'form', id: ITEM_ID + 3, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                ];
                const activeNodeId: number = ACTIVE_NODE_ID;

                spyOn(decisionModalsService, 'selectItemsToDelete').and.returnValue(Promise.resolve({
                    delete: items,
                    deleteForms: { [ITEM_ID + 1]: ['de'], [ITEM_ID + 2]: [], [ITEM_ID + 3]: ['en'] },
                    localizations: [],
                    unlocalize: [],
                }));

                const expectedResult: number[] = [ITEM_ID + 1, ITEM_ID + 3];
                const removedItemIds = await contextMenuOperationsService.deleteItems(type, items, activeNodeId);

                expect(removedItemIds).toEqual(expectedResult);
                expect(wastebinActionsMoveItemsToWastebinSpy).not.toHaveBeenCalled();

                expect(folderActionsUpdateItemSpy).toHaveBeenCalledTimes(2);
                expect(folderActionsUpdateItemSpy.calls.allArgs()[0]).toEqual([type, ITEM_ID + 1, { data: basicFormDataWithoutDe, languages: ['en'] }]);
                expect(folderActionsUpdateItemSpy.calls.allArgs()[1]).toEqual([type, ITEM_ID + 3, { data: basicFormDataWithoutEn, languages: ['de'] }]);
            });

            it('will only trigger deletions, if all languages of a form are deleted', async () => {
                const type: FolderItemType = 'form';
                const items: Form[] = [
                    { name: 'Some Form 1', type: 'form', id: ITEM_ID + 1, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                    { name: 'Some Form 2', type: 'form', id: ITEM_ID + 2, languages: ['en'], data: cloneDeep(basicFormDataWithoutDe) } as Form,
                    { name: 'Some Form 3', type: 'form', id: ITEM_ID + 3, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                ];
                const activeNodeId: number = ACTIVE_NODE_ID;

                spyOn(decisionModalsService, 'selectItemsToDelete').and.returnValue(Promise.resolve({
                    delete: items,
                    deleteForms: { [ITEM_ID + 1]: ['en', 'de'], [ITEM_ID + 2]: ['en'], [ITEM_ID + 3]: ['en', 'de'] },
                    localizations: [],
                    unlocalize: [],
                }));

                const expectedResult: number[] = [ITEM_ID + 1, ITEM_ID + 2, ITEM_ID + 3];
                const removedItemIds = await contextMenuOperationsService.deleteItems(type, items, activeNodeId);

                expect(removedItemIds).toEqual(expectedResult);
                expect(wastebinActionsMoveItemsToWastebinSpy).toHaveBeenCalledTimes(1);
                expect(wastebinActionsMoveItemsToWastebinSpy).toHaveBeenCalledWith(type, [ITEM_ID + 1, ITEM_ID + 2, ITEM_ID + 3], ACTIVE_NODE_ID, true);

                expect(folderActionsUpdateItemSpy).not.toHaveBeenCalled();
            });

            it('will trigger updates for forms where not all languages are deleted and deletions where all languages are deleted', async () => {
                const type: FolderItemType = 'form';
                const items: Form[] = [
                    { name: 'Some Form 1', type: 'form', id: ITEM_ID + 1, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                    { name: 'Some Form 2', type: 'form', id: ITEM_ID + 2, languages: ['en'], data: cloneDeep(basicFormDataWithoutDe) } as Form,
                    { name: 'Some Form 3', type: 'form', id: ITEM_ID + 3, languages: ['en', 'de'], data: cloneDeep(basicFormData) } as Form,
                ];
                const activeNodeId: number = ACTIVE_NODE_ID;

                spyOn(decisionModalsService, 'selectItemsToDelete').and.returnValue(Promise.resolve({
                    delete: items,
                    deleteForms: { [ITEM_ID + 1]: ['en', 'de'], [ITEM_ID + 2]: [], [ITEM_ID + 3]: ['en'] },
                    localizations: [],
                    unlocalize: [],
                }));

                const expectedResult: number[] = [ITEM_ID + 1, ITEM_ID + 3];
                const removedItemIds = await contextMenuOperationsService.deleteItems(type, items, activeNodeId);

                expect(removedItemIds).toEqual(expectedResult);
                expect(wastebinActionsMoveItemsToWastebinSpy).toHaveBeenCalledTimes(1);
                expect(wastebinActionsMoveItemsToWastebinSpy).toHaveBeenCalledWith(type, [ITEM_ID + 1], ACTIVE_NODE_ID, false);

                expect(folderActionsUpdateItemSpy).toHaveBeenCalledTimes(1);
                expect(folderActionsUpdateItemSpy).toHaveBeenCalledWith(type, ITEM_ID + 3, { data: basicFormDataWithoutEn, languages: ['de'] });
            });
        });
    });

});

class MockDecisionModalsService {
    selectItemsToDelete(items: InheritableItem[]): Promise<MultiDeleteResult> {
        throw new Error('selectItemsToDelete called but not mocked');
    }
}

class MockFolderActions implements Partial<FolderActionsService> {
    createPageVariations(sourcePages: Page[], sourceNodeId: number, targetFolders: Folder[]): Promise<void> {
        throw new Error('createPageVariations called but not mocked');
    }
    localizeItem(type: 'folder', itemId: number, channelId: number): Promise<Folder<Raw>>;
    localizeItem(type: 'page', itemId: number, channelId: number): Promise<Page<Raw>>;
    localizeItem(type: 'file', itemId: number, channelId: number): Promise<File<Raw>>;
    localizeItem(type: 'image', itemId: number, channelId: number): Promise<Image<Raw>>;
    localizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<InheritableItem<Raw>>;
    localizeItem(type: FolderItemType, itemId: number, channelId: number): Promise<InheritableItem<Raw> | void> {
        return Promise.resolve(null);
    }
    refreshList(type: FolderItemType, itemLanguages?: string[]): Promise<void> {
        throw new Error('refreshList called but not mocked');
    }
    getTemplatesRaw(): Observable<any> {
        throw new Error('getTemplatesRaw called but not mocked');
    }
    getAllTemplatesOfNode(): Observable<any> {
        throw new Error('getAllTemplatesOfNode called but not mocked');
    }
    updateItem<T extends ItemType>(
        type: T,
        itemId: number,
        payload: Partial<ItemTypeMap<Raw>[T]>,
        requestOptions?: Partial<FolderItemOrNodeSaveOptionsMap[T]>,
        postUpdateBehavior?: PostUpdateBehavior,
    ): Promise<ItemTypeMap<Raw>[T] | void> {
        throw new Error('updateItem called but not mocked');
    }
    getTemplates(parentId: number, fetchAll: boolean = false, search: string = '', pageNumber = 1): Promise<Template[] | null>  {
        throw new Error('getTemplates called but not mocked');
    }
}

class MockI18nNotification implements Partial<I18nNotificationService> {
    show(options: TranslatedNotificationOptions): { dismiss: () => void; } {
        return { dismiss: () => {} };
    }
}

class MockPermissionService implements Partial<PermissionService> {
    wastebin$ = of(false);
}

class MockWastebinActions implements Partial<WastebinActionsService> {
    moveItemsToWastebin(
        type: 'folder' | 'page' | 'file' | 'image' | 'form',
        ids: number[],
        nodeId: number,
        disableInstant?: boolean,
    ): Promise<{ succeeded: number; failed: number; error: ApiError }> {
        throw new Error('moveItemsToWastebin called but not mocked');
    }
}

class MockErrorHandler { }

const mockInstructions: () => InstructionActions = () => ({
    commands: () => [],
    navigate: () => Promise.resolve(true),
    navigateIfNotSet: () => Promise.resolve(true),
});

class MockNavigationService {
    detail(): InstructionActions {
        return mockInstructions();
    }

    detailOrModal(): InstructionActions {
        return mockInstructions();
    }

    modal(): InstructionActions {
        return mockInstructions();
    }
}

class MockModalService implements Partial<ModalService> {
    dialog(config: IDialogConfig, options?: IModalOptions): Promise<IModalInstance<ModalDialogComponent>> {
        return Promise.resolve(null);
    }

    fromComponent<T extends IModalDialog>(
        component: Type<T>,
        options?: IModalOptions,
        locals?: { [K in keyof T]?: T[K] },
    ): Promise<IModalInstance<T>>{
        return Promise.resolve(null);
    }
}

class MockUsageActions {
    getTotalUsage(): void { }
}


class MockRepositoryBrowserClientService implements Partial<RepositoryBrowserClient> {
    openRepositoryBrowser<T extends AllowedSelectionType, R = AllowedSelectionTypeMap[T]>(
        options: RepositoryBrowserOptions & { allowedSelection: T, selectMultiple: false }
    ): Promise<R>;
    openRepositoryBrowser<T extends AllowedSelectionType, R = AllowedSelectionTypeMap[T]>(
        options: RepositoryBrowserOptions & { allowedSelection: T, selectMultiple: true }
    ): Promise<R[]>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(
        options: RepositoryBrowserOptions & { allowedSelection: AllowedSelectionType[], selectMultiple: false }
    ): Promise<R>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(
        options: RepositoryBrowserOptions & { allowedSelection: AllowedSelectionType[], selectMultiple: true }
    ): Promise<R[]>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(options: RepositoryBrowserOptions): Promise<R | R[]>;
    openRepositoryBrowser<R = ItemInNode | TagInContainer>(options: RepositoryBrowserOptions): Promise<R | R[]>  {
        return Promise.resolve([]);
    }
}

class MockLinkTemplateService {
    setTemplatesOfFolder(): void { }
}

class MockFavouritesService { }

class MockRouter {
    navigateByUrl(): void { }
}

class MockI18nService {
    translate(key: string, params?: any): string {
        return key;
    }
}

class MockTemplateActions implements Partial<TemplateActionsService> {
    linkTemplatesToFolders(
        nodeId: number,
        templateIds: number[],
        folderIds: number[],
        recursive: boolean = false,
    ): Observable<boolean>  {
        return of(true);
    }
}

class MockContentStagingActions {

}
