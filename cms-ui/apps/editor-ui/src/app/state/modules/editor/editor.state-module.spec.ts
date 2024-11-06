import { TestBed } from '@angular/core/testing';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Normalized, Page, PageVersion, PrivilegeMap, User } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { EditorState, ITEM_PROPERTIES_TAB } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState, replaceInState } from '../../test-application-state.mock';
import { LockItemAction, ResetPageLockAction } from '../entity/entity.actions';
import { STATE_MODULES } from '../state-modules';
import {
    CancelEditingAction,
    ChangeTabAction,
    CloseEditorAction,
    ComparePageVersionsAction,
    EditItemAction,
    MarkContentAsModifiedAction,
    MarkObjectPropertiesAsModifiedAction,
    PreviewPageVersionAction,
    SaveErrorAction,
    SaveSuccessAction,
    SetFocusModeAction,
    SetUploadStatusAction,
    StartSavingAction,
} from './editor.actions';

describe('EditorStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
    });

    it('sets the correct initial state', () => {
        expect(appState.now.editor).toEqual({
            compareWithId: undefined,
            editorIsFocused: false,
            editorIsOpen: false,
            focusMode: false,
            fetching: false,
            saving: false,
            lastError: '',
            openTab: 'properties',
            openPropertiesTab: ITEM_PROPERTIES_TAB,
            openObjectPropertyGroups: [],
            contentModified: false,
            objectPropertiesModified: false,
            modifiedObjectPropertiesValid: false,
            uploadInProgress: false,
        } as EditorState);
    });

    it('cancelEditing works', () => {
        const objId = 1234;

        appState.mockState({
            entities: {
                page: {
                    [objId]: {
                        id: objId,
                        locked: true,
                        lockedBy: 1,
                        lockedSince: 1000205200,
                        name: 'Page 1',
                    } as Partial<Page<Normalized>> as Page<Normalized>,
                },
            },
        });
        appState.dispatch(new CancelEditingAction());
        appState.dispatch(new ResetPageLockAction(objId));

        expect(appState.now.entities.page[objId]).toEqual(
            jasmine.objectContaining({
                id: objId,
                locked: false,
                lockedBy: null,
                lockedSince: -1,
            }),
        );
    });

    it('changeTab works', () => {
        appState.mockState({
            editor: {
                openTab: 'preview',
            },
        });

        appState.dispatch(new ChangeTabAction('properties'));
        expect(appState.now.editor.openTab).toBe('properties');

        appState.dispatch(new ChangeTabAction('invalid tab' as any));
        expect(appState.now.editor.openTab).toBe('properties');
    });

    it('closeEditor works', () => {
        appState.mockState({
            editor: {
                editorIsOpen: true,
            },
        });
        appState.dispatch(new CloseEditorAction());
        expect(appState.now.editor.editorIsOpen).toBe(false);
    });

    it('enableFocusMode works', () => {
        appState.mockState({
            editor: {
                focusMode: false,
            },
        });
        appState.dispatch(new SetFocusModeAction(true));
        expect(appState.now.editor.focusMode).toBe(true);
    });

    it('disableFocusMode works', () => {
        appState.mockState({
            editor: {
                focusMode: true,
            },
        });
        appState.dispatch(new SetFocusModeAction(false));
        expect(appState.now.editor.focusMode).toBe(false);
    });

    it('comparePageVersions works', () => {
        const itemId = 1234;
        const nodeId = 1;
        const oldVersion = {
            // eslint-disable-next-line id-blacklist
            number: '1.4',
            timestamp: 1234567890,
        } as Partial<PageVersion> as PageVersion;
        const newVersion = {
            // eslint-disable-next-line id-blacklist
            number: '2.0',
            timestamp: 1234555555,
        } as Partial<PageVersion> as PageVersion;

        appState.dispatch(new ComparePageVersionsAction(itemId, nodeId, oldVersion, newVersion));

        expect(appState.now.editor).toEqual(
            jasmine.objectContaining({
                compareWithId: undefined,
                editMode: 'compareVersionContents',
                editorIsOpen: true,
                itemType: 'page',
                itemId: itemId,
                nodeId: nodeId,
                oldVersion: oldVersion,
                version: newVersion,
            } as EditorState),
        );
    });

    describe('editItem', () => {

        const PAGEID = 111;
        const FOLDERID = 222;
        const USERID = 333;
        let folderPrivileges: PrivilegeMap = undefined as any;

        beforeEach(() => {
            folderPrivileges = {
                languages: { },
                privileges: {
                    updatepage: true,
                },
            } as PrivilegeMap;

            appState.mockState({
                auth: {
                    currentUserId: USERID,
                },
                entities: {
                    folder: {
                        [FOLDERID]: {
                            id: FOLDERID,
                            type: 'folder',
                            privilegeMap: folderPrivileges,
                        },
                    },
                    page: {
                        [PAGEID]: {
                            id: PAGEID,
                            type: 'page',
                            folderId: FOLDERID,
                            locked: false,
                            lockedSince: -1,
                            lockedBy: undefined,
                        } as Partial<Page<Normalized>> as Page<Normalized>,
                    },
                    user: {
                        [USERID]: {
                            id: USERID,
                            firstName: 'John',
                            lastName: 'Doe',
                        } as Partial<User<Normalized>> as User<Normalized>,
                    },
                },
            });
        });

        it('works for previewing a page', () => {
            appState.dispatch(new EditItemAction({
                editMode: EditMode.PREVIEW,
                itemId: PAGEID,
                itemType: 'page',
                nodeId: 1,
                openTab: 'properties',
            }));
            appState.dispatch(new LockItemAction('page', PAGEID, EditMode.PREVIEW));

            expect(appState.now.editor).toEqual(
                jasmine.objectContaining({
                    editMode: EditMode.PREVIEW,
                    editorIsOpen: true,
                    itemId: PAGEID,
                    itemType: 'page',
                    nodeId: 1,
                    openTab: 'properties',
                    openPropertiesTab: undefined,
                }),
            );

            expect(appState.now.entities.page[PAGEID]).toEqual(
                jasmine.objectContaining({
                    locked: false,
                    lockedSince: -1,
                    lockedBy: undefined,
                }),
                'Page is locked by previewing, but should not be',
            );
        });

        it('works for editing a page', () => {
            appState.dispatch(new EditItemAction({
                editMode: EditMode.EDIT,
                itemId: PAGEID,
                itemType: 'page',
                nodeId: 1,
                openTab: 'properties',
            }));

            expect(appState.now.editor).toEqual(
                jasmine.objectContaining({
                    editMode: EditMode.EDIT,
                    editorIsOpen: true,
                    itemId: PAGEID,
                    itemType: 'page',
                    nodeId: 1,
                    openTab: 'properties',
                    openPropertiesTab: undefined,
                }),
            );
        });

        it('locks a page when editing it', () => {
            appState.dispatch(new EditItemAction({
                editMode: EditMode.EDIT,
                itemId: PAGEID,
                itemType: 'page',
                nodeId: 1,
                openTab: 'properties',
            }));
            appState.dispatch(new LockItemAction('page', PAGEID, EditMode.EDIT));

            expect(appState.now.entities.page[PAGEID]).toEqual(
                jasmine.objectContaining({
                    locked: true,
                    lockedSince: Math.round(Date.now() / 1000),
                    lockedBy: USERID,
                }),
            );
        });

        it('works for editing the properties of a folder', () => {
            appState.dispatch(new EditItemAction({
                editMode: EditMode.EDIT_PROPERTIES,
                itemId: FOLDERID,
                itemType: 'folder',
                nodeId: 2,
                openTab: 'properties',
                openPropertiesTab: ITEM_PROPERTIES_TAB,
            }));

            expect(appState.now.editor).toEqual(
                jasmine.objectContaining({
                    editMode: EditMode.EDIT_PROPERTIES,
                    editorIsOpen: true,
                    itemId: FOLDERID,
                    itemType: 'folder',
                    nodeId: 2,
                    openTab: 'properties',
                    openPropertiesTab: ITEM_PROPERTIES_TAB,
                }),
            );
        });

        it('locks a page when editing its properties', () => {
            expect(appState.now.entities.page[PAGEID].locked).toBe(false);

            appState.dispatch(new LockItemAction('page', PAGEID, EditMode.EDIT_PROPERTIES));

            expect(appState.now.entities.page[PAGEID].locked).toBe(true);
        });

        it('does not lock a page when the user has no edit privileges', () => {
            appState.mockState({
                entities: {
                    folder: {
                        [FOLDERID]: {
                            id: FOLDERID,
                            type: 'folder',
                            privilegeMap: {
                                languages: replaceInState({}),
                                privileges: {
                                    updatepage: false,
                                } as any,
                            },
                        },
                    },
                },
            });

            expect(appState.now.entities.page[PAGEID].locked).toBe(false, 'locked before doing anything');

            appState.dispatch(new LockItemAction('page', PAGEID, EditMode.EDIT_PROPERTIES));

            expect(appState.now.entities.page[PAGEID].locked).toBe(false, 'locked by editProperties');

            appState.dispatch(new LockItemAction('page', PAGEID, EditMode.EDIT));

            expect(appState.now.entities.page[PAGEID].locked).toBe(false, 'locked by edit');
        });

    });

    it('previewPageVersion works', () => {
        const version = {
            // eslint-disable-next-line id-blacklist
            number: '1.3',
            timestamp: 1234567890,
        } as Partial<PageVersion> as PageVersion;

        appState.dispatch(new PreviewPageVersionAction(23, 3, version));

        expect(appState.now.editor).toEqual(
            jasmine.objectContaining({
                compareWithId: undefined,
                editMode: EditMode.PREVIEW_VERSION,
                editorIsOpen: true,
                itemId: 23,
                itemType: 'page',
                nodeId: 3,
                oldVersion: undefined,
                version: version,
            } as EditorState),
        );

        expect(appState.now.entities.page[23])
            .not.toEqual(jasmine.objectContaining({ locked: true }), 'Page gets locked but should not be');
    });

    it('markContentAsModified works', () => {
        appState.dispatch(new MarkContentAsModifiedAction(true));
        expect(appState.now.editor.contentModified).toBe(true);
        expect(appState.now.editor.objectPropertiesModified).toBe(false);

        appState.dispatch(new MarkContentAsModifiedAction(false));
        expect(appState.now.editor.contentModified).toBe(false);
        expect(appState.now.editor.objectPropertiesModified).toBe(false);
    });

    it('markObjectPropertiesAsModified works', () => {
        appState.dispatch(new MarkObjectPropertiesAsModifiedAction(true, true));
        expect(appState.now.editor.contentModified).toBe(false);
        expect(appState.now.editor.objectPropertiesModified).toBe(true);
        expect(appState.now.editor.modifiedObjectPropertiesValid).toBe(true);

        appState.dispatch(new MarkObjectPropertiesAsModifiedAction(true, false));
        expect(appState.now.editor.contentModified).toBe(false);
        expect(appState.now.editor.objectPropertiesModified).toBe(true);
        expect(appState.now.editor.modifiedObjectPropertiesValid).toBe(false);

        appState.dispatch(new MarkObjectPropertiesAsModifiedAction(false, false));
        expect(appState.now.editor.contentModified).toBe(false);
        expect(appState.now.editor.objectPropertiesModified).toBe(false);
        expect(appState.now.editor.modifiedObjectPropertiesValid).toBe(false);
    });

    it('setUploadInProgress works', () => {
        appState.dispatch(new SetUploadStatusAction(true));
        expect(appState.now.editor.uploadInProgress).toBe(true);
    });

    it('unsetUploadInProgress', () => {
        appState.dispatch(new SetUploadStatusAction(false));
        expect(appState.now.editor.uploadInProgress).toBe(false);
    });

    it('saving works', () => {
        expect(appState.now.editor.saving).toBe(false);
        const mockErrorMessage = 'Error message';

        appState.dispatch(new StartSavingAction());
        expect(appState.now.editor.saving).toBe(true);

        appState.dispatch(new SaveSuccessAction());
        expect(appState.now.editor.saving).toBe(false);

        appState.dispatch(new StartSavingAction());
        expect(appState.now.editor.saving).toBe(true);

        appState.dispatch(new SaveErrorAction(mockErrorMessage));
        expect(appState.now.editor.saving).toBe(false);
    });

});
