import { TestBed } from '@angular/core/testing';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import {
    EditorPermissions,
    Folder,
    FolderPermissions,
    Normalized,
    PagePermissions,
    PermissionResponse, PermissionsMapCollection,
    PrivilegeMap,
} from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Observable, of as observableOf } from 'rxjs';
import { first } from 'rxjs/operators';
import { getExampleFolderData } from '@gentics/cms-models/testing/test-data.mock';
import { MockAppState, TestApplicationState } from '../../../state/test-application-state.mock';
import { Api } from '../api/api.service';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { PermissionService } from './permission.service';

const NODE = 11;
const FOLDER = 22;
const OTHERFOLDER = 33;
const PARENTFOLDER = 44;
const PAGE = 55;
const LANGUAGE = 1;
const LANGUAGE_STRING = 'de';
const OTHERLANGUAGE = 2;
const OTHERLANGUAGE_STRING = 'en';
const USER = 1234;

function takeOneValue<T>(stream: Observable<T>): T {
    let result: T;
    let sub = stream.subscribe(res => result = res);
    sub.unsubscribe();
    return result;
}

function mapWithPermissions(allow: boolean): PermissionsMapCollection {
    const result: PermissionsMapCollection = {
        permissions: {
            create: allow,
            createform: allow,
            createitems: allow,
            createtemplates: allow,
            deletefolder: allow,
            deleteform: allow,
            deleteitems: allow,
            deletetemplates: allow,
            importitems: allow,
            linktemplates: allow,
            publishform: allow,
            publishpages: allow,
            read: allow,
            readitems: allow,
            readtemplates: allow,
            setperm: allow,
            updatefolder: allow,
            updateform: allow,
            updateitems: allow,
            updatetemplates: allow,
            viewform: allow,
        },
        rolePermissions: {
            file: {
                createitems: allow,
                deleteitems: allow,
                readitems: allow,
                updateitems: allow,
            },
            page: {
                createitems: allow,
                deleteitems: allow,
                publishpages: allow,
                readitems: allow,
                translatepages: allow,
                updateitems: allow,
            },
            pageLanguages: {
                [LANGUAGE_STRING]: {
                    createitems: allow,
                    deleteitems: allow,
                    publishpages: allow,
                    readitems: allow,
                    translatepages: allow,
                    updateitems: allow,
                },
                [OTHERLANGUAGE_STRING]: {
                    createitems: allow,
                    deleteitems: allow,
                    publishpages: allow,
                    readitems: allow,
                    translatepages: allow,
                    updateitems: allow,
                },
            },
        },
    };

    return result;
}

const privilegesWithPermissions = (allow: boolean): PrivilegeMap => ({
    privileges: {
        viewfolder: allow,
        createfolder: allow,
        updatefolder: allow,
        deletefolder: allow,
        assignpermissions: allow,
        viewpage: allow,
        createpage: allow,
        updatepage: allow,
        deletepage: allow,
        publishpage: allow,
        viewfile: allow,
        createfile: allow,
        updatefile: allow,
        deletefile: allow,
        viewtemplate: allow,
        createtemplate: allow,
        linktemplate: allow,
        updatetemplate: allow,
        deletetemplate: allow,
        updatetagtypes: allow,
        inheritance: allow,
        importpage: allow,
        linkworkflow: allow,
        synchronizechannel: allow,
        wastebin: allow,
        translatepage: allow,
        viewform: allow,
        createform: allow,
        updateform: allow,
        deleteform: allow,
        publishform: allow,
        formreport: allow,
    },
    languages: {
        [LANGUAGE]: {
            viewpage: allow,
            createpage: allow,
            updatepage: allow,
            deletepage: allow,
            publishpage: allow,
            translatepage: allow,
            viewfile: allow,
            createfile: allow,
            updatefile: allow,
            deletefile: allow,
        },
        [OTHERLANGUAGE]: {
            viewpage: allow,
            createpage: allow,
            updatepage: allow,
            deletepage: allow,
            publishpage: allow,
            translatepage: allow,
            viewfile: allow,
            createfile: allow,
            updatefile: allow,
            deletefile: allow,
        },
    },
});

declare class AccessToProtectedMethod extends PermissionService {
    public mapToPermissions(priv: PrivilegeMap, map: PermissionsMapCollection, language?: number): EditorPermissions;
}

class MockApi {

    folders = {
        getItem(id: number, type: string, options: { nodeId: number }): Observable<any> {
            throw new Error('getItem called but not mocked');
        },
    };

    permissions = {
        resetTestData: () => {
            const reset = (data: { [key: string]: boolean }) => {
                for (const key in data) {
                    if (Object.prototype.hasOwnProperty.call(data, key)) {
                        data[key] = false;
                    }
                }
            };
            reset(this.permissions.default.privilegeMap.privileges);
            reset(this.permissions.default.privilegeMap.languages[0].privileges);
            reset(this.permissions.default.privilegeMap.languages[1].privileges);
            reset(this.permissions.default.permissionsMap.permissions);
            reset(this.permissions.default.permissionsMap.rolePermissions.file);
            reset(this.permissions.default.permissionsMap.rolePermissions.page);
            reset(this.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING]);
            reset(this.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING]);
        },
        default: {
            perm: null,
            privilegeMap: {
                privileges: {
                    viewfolder: false,
                    createfolder: false,
                    updatefolder: false,
                    deletefolder: false,
                    assignpermissions: false,
                    viewpage: false,
                    createpage: false,
                    updatepage: false,
                    deletepage: false,
                    publishpage: false,
                    viewfile: false,
                    createfile: false,
                    updatefile: false,
                    deletefile: false,
                    viewtemplate: false,
                    createtemplate: false,
                    linktemplate: false,
                    updatetemplate: false,
                    deletetemplate: false,
                    updatetagtypes: false,
                    inheritance: false,
                    importpage: false,
                    linkworkflow: false,
                    synchronizechannel: false,
                    wastebin: false,
                    translatepage: false,
                    viewform: false,
                    createform: false,
                    updateform: false,
                    deleteform: false,
                    publishform: false,
                },
                languages: [
                    {
                        language: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        privileges: {
                            viewpage: false,
                            createpage: false,
                            updatepage: false,
                            deletepage: false,
                            publishpage: false,
                            translatepage: false,
                            viewfile: false,
                            createfile: false,
                            updatefile: false,
                            deletefile: false,
                        },
                    },
                    {
                        language: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                        privileges: {
                            viewpage: false,
                            createpage: false,
                            updatepage: false,
                            deletepage: false,
                            publishpage: false,
                            translatepage: false,
                            viewfile: false,
                            createfile: false,
                            updatefile: false,
                            deletefile: false,
                        },
                    },
                ],
            },
            permissionsMap: mapWithPermissions(false),
            responseInfo: null,
        },
        getFolderPermissions(folderId: number, nodeId: number): Observable<PermissionResponse> {
            return observableOf(this.default).pipe(first());
        },
        getInboxPermissions(): Observable<any> {
            throw new Error('getInboxPermissions called but not mocked');
        },
        getPublishQueuePermissions(): Observable<any> {
            throw new Error('getPublishQueuePermissions called but not mocked');
        },
    };
}

describe('PermissionService', () => {

    let permissions: PermissionService;
    let state: TestApplicationState;
    let api: MockApi;
    let entityResolver: EntityResolver;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });

        state = TestBed.get(ApplicationStateService);
        api = new MockApi();
        entityResolver = new EntityResolver(state);
        permissions = new PermissionService(
            state,
            api as any as Api,
            entityResolver,
        );
    });

    function mockFolderChange(newFolderId: number): void {
        state.mockState({ folder: { ...state.now.folder, activeFolder: newFolderId } });
    }

    function mockLanguageChange(newLanguageId: number): void {
        state.mockState({ folder: { ...state.now.folder, activeLanguage: newLanguageId } });
    }

    function mockUserChange(newUserId: number): void {
        state.mockState({ auth: { ...state.now.auth, currentUserId: newUserId } });
    }

    describe('all$', () => {

        let testState: MockAppState;
        let firstFolder: Partial<Folder<Normalized>> = {
            privilegeMap: privilegesWithPermissions(false),
            permissionsMap: mapWithPermissions(false),
        };
        let secondFolder: Partial<Folder<Normalized>> = {
            privilegeMap: privilegesWithPermissions(false),
            permissionsMap: mapWithPermissions(false),
        };

        beforeEach(() => {
            testState = {
                auth: {
                    isLoggedIn: true,
                    currentUserId: USER,
                },
                entities: {
                    folder: {
                        [FOLDER]: firstFolder,
                        [OTHERFOLDER]: secondFolder,
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeFolder: FOLDER,
                    activeLanguage: LANGUAGE,
                    activeNode: NODE,
                    activeNodeLanguages: {
                        list: [LANGUAGE, OTHERLANGUAGE],
                        total: 2,
                    },
                },
            };
        });

        it('emits permissions for items in the current folder', () => {
            state.mockState(testState);
            let perms = takeOneValue(permissions.all$);
            expect(perms.file.create).toBe(false);
            expect(perms.folder.create).toBe(false);
            expect(perms.page.create).toBe(false);
            expect(perms.template.create).toBe(false);
            expect(perms.file.delete).toBe(false);
            expect(perms.folder.delete).toBe(false);
            expect(perms.page.delete).toBe(false);
            expect(perms.template.delete).toBe(false);
            expect(perms.page.import).toBe(false);
            expect(perms.template.link).toBe(false);
            expect(perms.page.publish).toBe(false);
            expect(perms.file.edit).toBe(false);
            expect(perms.image.edit).toBe(false);
            expect(perms.folder.edit).toBe(false);
            expect(perms.page.edit).toBe(false);
            expect(perms.tagType.create).toBe(false);
            expect(perms.tagType.delete).toBe(false);
            expect(perms.tagType.edit).toBe(false);
            expect(perms.tagType.view).toBe(false);
            expect(perms.template.edit).toBe(false);
            expect(perms.file.view).toBe(false);
            expect(perms.image.view).toBe(false);
            expect(perms.folder.view).toBe(false);
            expect(perms.page.view).toBe(false);
            expect(perms.template.view).toBe(false);
            expect(perms.page.translate).toBe(false);
        });

        // this test makes the same as below: 'emits after changing language when permissions change'
        xit('emits after switching folder when permissions change', () => {
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].publishpages = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].publishpages = true;

            state.mockState(testState);
            const emitted = [] as EditorPermissions[];
            const sub = permissions.all$.subscribe(perms => emitted.push(perms));

            expect(emitted.length).toBe(1);
            expect(emitted[0].page.create).toBe(true);
            expect(emitted[0].page.publish).toBe(false);

            mockLanguageChange(OTHERLANGUAGE);

            expect(emitted.length).toBe(2);
            expect(emitted[1].page.create).toBe(false);
            expect(emitted[1].page.publish).toBe(true);

            sub.unsubscribe();
        });

        it('merges group permissions of the current language into the result', () => {
            api.permissions.default.permissionsMap.rolePermissions.file.createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.page.createitems = false;

            state.mockState(testState);
            const perms = takeOneValue(permissions.all$);

            expect(perms.file.create).toBe(true);
            expect(perms.page.create).toBe(false);

        });

        it('emits after changing language when permissions change', () => {
            api.permissions.default.permissionsMap.permissions.createitems = false;
            api.permissions.default.permissionsMap.permissions.publishpages = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].publishpages = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].publishpages = true;

            state.mockState(testState);
            const emitted = [] as EditorPermissions[];
            const sub = permissions.all$.subscribe(perms => emitted.push(perms));

            expect(emitted.length).toBe(1);
            expect(emitted[0].page.create).toBe(true);
            expect(emitted[0].page.publish).toBe(false);

            mockLanguageChange(OTHERLANGUAGE);

            expect(emitted.length).toBe(2);
            expect(emitted[1].page.create).toBe(false);
            expect(emitted[1].page.publish).toBe(true);

            sub.unsubscribe();
        });

        it('emits after changing user and loading the permissions again', () => {
            api.permissions.default.permissionsMap.permissions.createitems = false;
            api.permissions.default.permissionsMap.permissions.deleteitems = true;

            state.mockState(testState);
            const emitted = [] as EditorPermissions[];
            let sub = permissions.all$.subscribe(perms => {
                emitted.push(perms);
            });

            expect(emitted.length).toBe(1, 'permissions emitted != 1 time');
            expect(emitted[0].page.create).toBe(false);
            expect(emitted[0].page.delete).toBe(true);

            // Logout
            state.mockState({
                auth: { ...state.now.auth, isLoggedIn: false, currentUserId: null },
                entities: { ...state.now.entities, folder: {} },
            });
            expect(emitted.length).toBe(1, 're-emitted after logout');
            sub.unsubscribe();

            // change permissions
            api.permissions.default.permissionsMap.permissions.createitems = true;
            api.permissions.default.permissionsMap.permissions.deleteitems = false;

            sub = permissions.all$.subscribe(perms => {
                emitted.push(perms);
            });

            // Login as second user
            state.mockState({
                auth: { ...state.now.auth, isLoggedIn: true, currentUserId: USER + 1 },
            });

            expect(emitted.length).toBe(3);
            expect(emitted[2].page.create).toBe(true);
            expect(emitted[2].page.delete).toBe(false);

            sub.unsubscribe();
        });

        it('does not emit after changing language when permissions did not change', () => {
            api.permissions.default.permissionsMap.permissions.createitems = false;
            api.permissions.default.permissionsMap.permissions.publishpages = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].publishpages = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].publishpages = false;

            state.mockState(testState);
            const emitted = [] as EditorPermissions[];
            const sub = permissions.all$.subscribe(perms => emitted.push(perms));

            expect(emitted.length).toBe(1);
            expect(emitted[0].page.create).toBe(true);
            expect(emitted[0].page.publish).toBe(false);

            mockLanguageChange(OTHERLANGUAGE);
            expect(emitted.length).toBe(1);

            sub.unsubscribe();
        });

        it('emits after loading initial folder', () => {
            const emitted = [] as EditorPermissions[];
            const sub = permissions.all$.subscribe(perms => emitted.push(perms));

            expect(emitted.length).toBe(0);

            let permissionsMap = mapWithPermissions(false);
            api.permissions.default.permissionsMap.permissions.createitems = true;
            api.permissions.default.permissionsMap.permissions.publishpages = true;

            state.mockState(testState);
            state.mockState({
                entities: {
                    folder: {
                        [FOLDER]: { permissionsMap },
                    },
                },
            });

            expect(emitted.length).toBe(1);
            expect(emitted[0].file.create).toBe(true);
            expect(emitted[0].page.publish).toBe(true);

            sub.unsubscribe();
        });

        afterEach(() => {
            api.permissions.resetTestData();
        });

    });

    describe('mapped observables', () => {

        let testState: MockAppState;

        beforeEach(() => {
            testState = {
                auth: {
                    isLoggedIn: true,
                    currentUserId: USER,
                },
                entities: {
                    folder: {
                        [FOLDER]: {
                            privilegeMap: privilegesWithPermissions(false),
                            permissionsMap: mapWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeFolder: FOLDER,
                    activeLanguage: LANGUAGE,
                    activeNode: NODE,
                    activeNodeLanguages: {
                        list: [LANGUAGE, OTHERLANGUAGE],
                        total: 2,
                    },
                },
            };
        });

        it('assignPermissions$', () => {
            api.permissions.default.permissionsMap.permissions.setperm = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.assignPermissions$)).toBe(true);
        });

        it('file.create$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.create$)).toBe(true);
        });

        it('file.delete$', () => {
            api.permissions.default.permissionsMap.permissions.deleteitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.delete$)).toBe(true);
        });

        it('file.edit$', () => {
            api.permissions.default.permissionsMap.permissions.updateitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.edit$)).toBe(true);
        });

        it('file.import$', () => {
            api.permissions.default.permissionsMap.permissions.importitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.import$)).toBe(true);
        });

        it('file.inherit$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.inheritance = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.inherit$)).toBe(true);
        });

        it('file.localize$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.localize$)).toBe(true);
        });

        it('file.publish$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.file.publish$)).toBe(false);
        });

        it('file.translate$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.file.translate$)).toBe(false);
        });

        it('file.view$', () => {
            api.permissions.default.permissionsMap.permissions.readitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.file.view$)).toBe(true);
        });

        it('folder.create$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.createfolder = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.create$)).toBe(true);
        });

        it('folder.delete$', () => {
            api.permissions.default.permissionsMap.permissions.deletefolder = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.delete$)).toBe(true);
        });

        it('folder.edit$', () => {
            api.permissions.default.permissionsMap.permissions.updatefolder = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.edit$)).toBe(true);
        });

        it('folder.import$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.import$)).toBe(false);
        });

        it('folder.inherit$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.inheritance = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.inherit$)).toBe(true);
        });

        it('folder.localize$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.localize$)).toBe(true);
        });

        it('folder.publish$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.publish$)).toBe(false);
        });

        it('folder.translate$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.translate$)).toBe(false);
        });

        it('folder.view$', () => {
            api.permissions.default.permissionsMap.permissions.readitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.folder.view$)).toBe(true);
        });

        it('image.create$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.create$)).toBe(true);
        });

        it('image.delete$', () => {
            api.permissions.default.permissionsMap.permissions.deleteitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.delete$)).toBe(true);
        });

        it('image.edit$', () => {
            api.permissions.default.permissionsMap.permissions.updateitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.edit$)).toBe(true);
        });

        it('image.import$', () => {
            api.permissions.default.permissionsMap.permissions.importitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.import$)).toBe(true);
        });

        it('image.inherit$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.inheritance = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.inherit$)).toBe(true);
        });

        it('image.localize$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.localize$)).toBe(true);
        });

        it('image.publish$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.image.publish$)).toBe(false);
        });

        it('image.translate$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.image.translate$)).toBe(false);
        });

        it('image.view$', () => {
            api.permissions.default.permissionsMap.permissions.readitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.image.view$)).toBe(true);
        });

        it('linkTemplate$', () => {
            api.permissions.default.permissionsMap.permissions.linktemplates = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.linkTemplate$)).toBe(true);
        });

        it('page.create$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.create$)).toBe(true);
        });

        it('page.delete$', () => {
            api.permissions.default.permissionsMap.permissions.deleteitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.delete$)).toBe(true);
        });

        it('page.edit$', () => {
            api.permissions.default.permissionsMap.permissions.updateitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.edit$)).toBe(true);
        });

        it('page.import$', () => {
            api.permissions.default.permissionsMap.permissions.importitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.import$)).toBe(true);
        });

        it('page.inherit$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.inheritance = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.inherit$)).toBe(true);
        });

        it('page.localize$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.localize$)).toBe(true);
        });

        it('page.publish$', () => {
            api.permissions.default.permissionsMap.permissions.publishpages = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.publish$)).toBe(true);
        });

        it('page.translate$', () => {
            api.permissions.default.permissionsMap.permissions.createitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.translate$)).toBe(true);
        });

        it('page.view$', () => {
            api.permissions.default.permissionsMap.permissions.readitems = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.page.view$)).toBe(true);
        });

        it('synchronizeChannel$', () => {
            api.permissions.default.permissionsMap.permissions.channelsync = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.synchronizeChannel$)).toBe(true);
        });

        it('template.create$', () => {
            api.permissions.default.permissionsMap.permissions.createtemplates = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.template.create$)).toBe(true);
        });

        it('template.delete$', () => {
            api.permissions.default.permissionsMap.permissions.deletetemplates = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.template.delete$)).toBe(true);
        });

        it('template.edit$', () => {
            api.permissions.default.permissionsMap.permissions.updatetemplates = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.template.edit$)).toBe(true);
        });

        it('template.import$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.template.import$)).toBe(false);
        });

        it('template.inherit$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.inheritance = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.template.inherit$)).toBe(true);
        });

        it('template.localize$', () => {
            api.permissions.default.permissionsMap.permissions.createtemplates = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.template.localize$)).toBe(true);
        });

        it('template.publish$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.template.publish$)).toBe(false);
        });

        it('template.translate$ (always false)', () => {
            state.mockState(testState);
            expect(takeOneValue(permissions.template.translate$)).toBe(false);
        });

        it('template.view$', () => {
            api.permissions.default.permissionsMap.permissions.readtemplates = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.template.view$)).toBe(true);
        });

        it('viewInbox$', () => {
            let emitted = [] as any[];
            state.mockState(testState);

            api.permissions.getInboxPermissions = () => Observable.of({ view: false });
            permissions.viewInbox$.subscribe(v => emitted.push(v));
            expect(emitted).toEqual([false]);

            api.permissions.getInboxPermissions = () => Observable.of({ view: true });
            expect(emitted).toEqual([false], 'emitted twice');

            mockUserChange(USER + 1);
            expect(emitted).toEqual([false, true]);
        });

        it('viewPublishQueue$', () => {
            let emitted = [] as any[];
            state.mockState(testState);

            api.permissions.getPublishQueuePermissions = () => Observable.of({ view: false });
            permissions.viewPublishQueue$.subscribe(v => emitted.push(v));
            expect(emitted).toEqual([false]);

            api.permissions.getPublishQueuePermissions = () => Observable.of({ view: true });
            expect(emitted).toEqual([false], 'emitted twice');

            mockUserChange(USER + 1);
            expect(emitted).toEqual([false, true]);
        });

        it('wastebin$', () => {
            testState.entities.folder[FOLDER].privilegeMap.privileges.wastebin = true;
            state.mockState(testState);
            expect(takeOneValue(permissions.wastebin$)).toBe(true);
        });

        afterEach(() => {
            api.permissions.resetTestData();
        });

    });

    describe('forFolderInLanguage()', () => {

        let testState: MockAppState;

        beforeEach(() => {
            testState = {
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                            permissionsMap: mapWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
            };
        });

        it('returns the permissions from the app store if possible', () => {
            api.folders.getItem = jasmine.createSpy('getItem');
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions').and.returnValue(observableOf(api.permissions.default));

            testState.entities.folder[FOLDER].permissionsMap.permissions.createitems = true;
            state.mockState(testState);

            let perms$ = permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE);

            expect(api.folders.getItem).not.toHaveBeenCalled();
            expect(api.permissions.getFolderPermissions).toHaveBeenCalled();

            let perms: EditorPermissions = takeOneValue(perms$);
            expect(perms.page.create).toBe(true);
        });

        it('loads the folder permissions via the API if not in the app state', () => {
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions')
                .and.returnValue(Observable.never());

            state.mockState({
                entities: {},
            });

            permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE);
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledWith(FOLDER, NODE);
        });

        it('only requests the permissions from the API once (within a 10 seconds timeframe) when called multiple times, ' +
            'but emits latest value for each new subscriber', () => {
            let subscribed = 0;
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions')
                .and.returnValue(new Observable(subscriber => {
                    ++subscribed;
                    subscriber.next({
                        permissionsMap: getExampleFolderData().permissionsMap,
                    });
                }));

            let emissionsCount = 0;
            const subscriber = () => ++emissionsCount;

            state.mockState({
                entities: {
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
            });

            permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE).subscribe(subscriber);
            permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE).subscribe(subscriber);
            permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE).subscribe(subscriber);
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledTimes(1);
            expect(subscribed).toBe(1);
            expect(emissionsCount).toBe(3);

            permissions.forFolderInLanguage(FOLDER + 1, NODE, LANGUAGE).subscribe(subscriber);
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledTimes(2);
            expect(subscribed).toBe(2);
            expect(emissionsCount).toBe(4);

            permissions.forFolderInLanguage(FOLDER, NODE + 1, LANGUAGE).subscribe(subscriber);
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledTimes(3);
            expect(subscribed).toBe(3);
            expect(emissionsCount).toBe(5);
        });

        it('maps the permissions of the folder it was called for', () => {
            testState.entities.folder[FOLDER].permissionsMap.permissions.createitems = true;
            testState.entities.folder[FOLDER].permissionsMap.permissions.deleteitems = false;

            state.mockState(testState);

            let perms = takeOneValue(permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE));

            expect(perms.page.create).toBe(true);
            expect(perms.page.delete).toBe(false);
            expect(perms.file.create).toBe(true);
            expect(perms.file.delete).toBe(false);
            expect(perms.image.create).toBe(true);
            expect(perms.image.delete).toBe(false);
        });

        it('combines permissions with group permissions of the language it was called with', () => {
            testState.entities.folder[FOLDER].permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            testState.entities.folder[FOLDER].permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].deleteitems = false;
            testState.entities.folder[FOLDER].permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = false;
            testState.entities.folder[FOLDER].permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].deleteitems = true;

            state.mockState(testState);

            let perms = takeOneValue(permissions.forFolderInLanguage(FOLDER, NODE, LANGUAGE));

            expect(perms.page.create).toBe(true);
            expect(perms.page.delete).toBe(false);

            perms = takeOneValue(permissions.forFolderInLanguage(FOLDER, NODE, OTHERLANGUAGE));

            expect(perms.page.create).toBe(false);
            expect(perms.page.delete).toBe(true);
        });

        afterEach(() => {
            api.permissions.resetTestData();
        });
    });

    describe('forItemInLanguage()', () => {

        it('returns the permissions from the app store if possible', () => {
            api.folders.getItem = jasmine.createSpy('getItem');
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions').and.returnValue(observableOf(api.permissions.default));

            const testState: MockAppState = {
                entities: {
                    folder: {
                        [PARENTFOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                            permissionsMap: mapWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                    page: {
                        [PAGE]: {
                            folderId: PARENTFOLDER,
                        },
                    },
                },
            };
            testState.entities.folder[PARENTFOLDER].permissionsMap.permissions.createitems = true;
            state.mockState(testState);

            let perms$ = permissions.forItemInLanguage('page', PAGE, NODE, LANGUAGE);

            expect(api.folders.getItem).not.toHaveBeenCalled();
            expect(api.permissions.getFolderPermissions).not.toHaveBeenCalled();

            let perms: PagePermissions = takeOneValue(perms$);
            expect(perms.create).toBe(true);
        });

        it('loads the item via the API if it is not in the app state', () => {
            api.folders.getItem = jasmine.createSpy('getItem')
                .and.returnValue(Observable.never());

            state.mockState({
                entities: {},
            });

            permissions.forItemInLanguage('page', PAGE, NODE, LANGUAGE);
            expect(api.folders.getItem).toHaveBeenCalledWith(PAGE, 'page', { nodeId: NODE });
        });

        it('loads the parent folder permissions via the API if it is not in the app state', () => {
            api.folders.getItem = jasmine.createSpy('getItem');
            api.permissions.default.permissionsMap.permissions.createitems = true;
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions').and.returnValue(observableOf(api.permissions.default));

            const testState: MockAppState = {
                entities: {
                    folder: {
                        [PARENTFOLDER]: {
                            id: PARENTFOLDER,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    page: {
                        [PAGE]: {
                            folderId: PARENTFOLDER,
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
            };
            state.mockState(testState);

            let perms$ = permissions.forItemInLanguage('page', PAGE, NODE, LANGUAGE);
            let perms: PagePermissions = takeOneValue(perms$);

            expect(api.folders.getItem).not.toHaveBeenCalled();
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledWith(PARENTFOLDER, NODE);
            expect(perms.create).toBe(true);
        });

        it('returns permissions of the parent folder when called for a folder', () => {

            let testState: MockAppState = {
                entities: {
                    folder: {
                        [FOLDER]: {
                            motherId: PARENTFOLDER,
                            type: 'folder',
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                            permissionsMap: mapWithPermissions(false),
                        },
                        [PARENTFOLDER]: {
                            type: 'folder',
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                            permissionsMap: mapWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
            };
            testState.entities.folder[PARENTFOLDER].permissionsMap.permissions = {
                deletefolder: true,
                updatefolder: false,
            };

            state.mockState(testState);

            api.permissions.default.permissionsMap.permissions = {
                deletefolder: false,
                updatefolder: true,
            };

            let perms$ = permissions.forItemInLanguage('folder', FOLDER, NODE, LANGUAGE);
            let perms: FolderPermissions = takeOneValue(perms$);

            expect(perms.delete).toBe(true);
            expect(perms.edit).toBe(false);
        });

        it('merges page role permissions of the passed language', () => {

            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].deleteitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].deleteitems = true;

            const testState: MockAppState = {
                entities: {
                    folder: {
                        [PARENTFOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    page: {
                        [PAGE]: {
                            folderId: PARENTFOLDER,
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeLanguage: LANGUAGE,
                },
            };
            state.mockState(testState);

            let perms$ = permissions.forItemInLanguage('page', PAGE, NODE, LANGUAGE);
            let perms: PagePermissions = takeOneValue(perms$);

            expect(perms.create).toBe(true);
            expect(perms.delete).toBe(false);

            perms$ = permissions.forItemInLanguage('page', PAGE, NODE, OTHERLANGUAGE);
            perms = takeOneValue(perms$);

            expect(perms.create).toBe(false);
            expect(perms.delete).toBe(true);
        });

        afterEach(() => {
            api.permissions.resetTestData();
        });

    });

    describe('forFolder()', () => {

        it('returns the permissions from the app store if possible', () => {
            api.folders.getItem = jasmine.createSpy('getItem');
            api.permissions.default.permissionsMap.permissions.createitems = true;
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions').and.returnValue(observableOf(api.permissions.default));

            state.mockState({
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeNodeLanguages: {
                        total: 2,
                    },
                    activeLanguage: LANGUAGE,
                },
            });

            let perms$ = permissions.forFolder(FOLDER, NODE);
            let perms: EditorPermissions = takeOneValue(perms$);

            expect(api.folders.getItem).not.toHaveBeenCalled();
            expect(api.permissions.getFolderPermissions).toHaveBeenCalled();

            expect(perms.page.create).toBe(true);
        });

        it('only loads permissions from the API once when called multiple times', () => {
            let subscribed = 0;
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions')
                .and.returnValue(new Observable(() => { subscribed += 1; }));

            state.mockState({
                entities: {
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeNodeLanguages: {
                        total: 2,
                    },
                    activeLanguage: LANGUAGE,
                },
            });

            permissions.forFolder(FOLDER, NODE).subscribe();
            permissions.forFolder(FOLDER, NODE).subscribe();
            permissions.forFolder(FOLDER, NODE).subscribe();
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledTimes(1);
            expect(subscribed).toBe(1);

            permissions.forFolder(FOLDER + 1, NODE).subscribe();
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledTimes(2);
            expect(subscribed).toBe(2);

            permissions.forFolder(FOLDER, NODE + 1).subscribe();
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledTimes(3);
            expect(subscribed).toBe(3);
        });

        it('maps the permissions of the folder it was called for', () => {

            api.permissions.default.permissionsMap.permissions.createitems = true;
            api.permissions.default.permissionsMap.permissions.deleteitems = false;
            api.permissions.getFolderPermissions = jasmine.createSpy('getFolderPermissions').and.returnValue(observableOf(api.permissions.default));

            state.mockState({
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeNodeLanguages: {
                        total: 2,
                    },
                    activeLanguage: LANGUAGE,
                },
            });

            let perms = takeOneValue(permissions.forFolder(FOLDER, NODE));

            expect(perms.page.create).toBe(true);
            expect(perms.page.delete).toBe(false);
            expect(api.permissions.getFolderPermissions).toHaveBeenCalledWith(FOLDER, NODE);
        });

        it('combines permissions with group permissions of the current language', () => {
            api.permissions.default.permissionsMap.permissions.createitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;

            state.mockState({
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeLanguage: LANGUAGE,
                    activeNodeLanguages: {
                        total: 1,
                    },
                },
            });

            let perms = takeOneValue(permissions.forFolder(FOLDER, NODE));
            expect(perms.page.create).toBe(true);
        });

        it('returned Observable emits when the permissions differ after a language change', () => {

            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].deleteitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = false;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].deleteitems = true;

            state.mockState({
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeLanguage: LANGUAGE,
                    activeNodeLanguages: {
                        total: 1,
                    },
                },
            });

            let perms$ = permissions.forFolder(FOLDER, NODE);
            let emittedValues: EditorPermissions[] = [];
            let sub = perms$.subscribe(val => emittedValues.push(val));

            expect(emittedValues.length).toBe(1);
            expect(emittedValues[0].page.create).toBe(true);
            expect(emittedValues[0].page.delete).toBe(false);

            mockLanguageChange(OTHERLANGUAGE);

            expect(emittedValues.length).toBe(2);
            expect(emittedValues[1].page.create).toBe(false);
            expect(emittedValues[1].page.delete).toBe(true);

            sub.unsubscribe();
        });

        it('returned Observable does not emit after a language change without role permissions', () => {
            let folder: Partial<Folder<Normalized>> = {
                privilegeMap: {} as PrivilegeMap,
                permissionsMap: {} as PermissionsMapCollection,
            };

            state.mockState({
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                            permissionsMap: mapWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeLanguage: LANGUAGE,
                    activeNodeLanguages: {
                        total: 1,
                    },
                },
            });

            let perms$ = permissions.forFolder(FOLDER, NODE);
            let emittedValues: EditorPermissions[] = [];
            let sub = perms$.subscribe(val => emittedValues.push(val));

            expect(emittedValues.length).toBe(1);
            mockLanguageChange(OTHERLANGUAGE);
            expect(emittedValues.length).toBe(1);

            sub.unsubscribe();
        });

        it('returned Observable does emit after a language change when the permissions stay the same', () => {
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[LANGUAGE_STRING].createitems = true;
            api.permissions.default.permissionsMap.rolePermissions.pageLanguages[OTHERLANGUAGE_STRING].createitems = true;

            const testState: MockAppState = {
                entities: {
                    folder: {
                        [FOLDER]: {
                            nodeId: NODE,
                            privilegeMap: privilegesWithPermissions(false),
                        },
                    },
                    language: {
                        [LANGUAGE]: {
                            code: LANGUAGE_STRING,
                            id: LANGUAGE,
                            name: LANGUAGE_STRING,
                        },
                        [OTHERLANGUAGE]: {
                            code: OTHERLANGUAGE_STRING,
                            id: OTHERLANGUAGE,
                            name: OTHERLANGUAGE_STRING,
                        },
                    },
                },
                folder: {
                    activeLanguage: LANGUAGE,
                    activeNodeLanguages: {
                        total: 1,
                    },
                },
            };
            state.mockState(testState);

            let perms$ = permissions.forFolder(FOLDER, NODE);
            let emittedValues: EditorPermissions[] = [];
            let sub = perms$.subscribe(val => emittedValues.push(val));

            expect(emittedValues.length).toBe(1);
            expect(emittedValues[0].page.create).toBe(true);
            expect(emittedValues[0].page.delete).toBe(false);

            sub.unsubscribe();

            testState.folder.activeFolder = OTHERLANGUAGE;
            state.mockState(testState);
            sub = perms$.subscribe(val => emittedValues.push(val));
            expect(emittedValues.length).toBe(2);

            sub.unsubscribe();
        });

        afterEach(() => {
            api.permissions.resetTestData();
        });

    });

    describe('mapToPermissions()', () => {

        const mapToPermissions = (priv: PrivilegeMap, map: PermissionsMapCollection, language?: number): EditorPermissions =>
            (permissions as AccessToProtectedMethod).mapToPermissions(priv, map, language);

        // Left: used in app - right: returned from server
        const mappings = {
            assignPermissions: 'setperm',
            'file.create': 'createitems',
            'file.delete': 'deleteitems',
            'file.edit': 'updateitems',
            'file.import': 'importitems',
            'file.inherit': 'updateinheritance',
            'file.localize': 'createitems',
            'file.upload': 'createitems',
            'file.unlocalize': 'createitems',
            'file.view': 'readitems',
            'folder.create': 'createitems',
            'folder.delete': 'deletefolder',
            'folder.edit': 'updatefolder',
            'folder.inherit': 'updateinheritance',
            'folder.localize': 'createitems',
            'folder.unlocalize': 'deletefolder',
            'folder.view': 'readitems',
            'image.create': 'createitems',
            'image.delete': 'deleteitems',
            'image.edit': 'updateitems',
            'image.import': 'importitems',
            'image.inherit': 'updateinheritance',
            'image.localize': 'createitems',
            'image.upload': 'createitems',
            'image.unlocalize': 'createitems',
            'image.view': 'readitems',
            'page.create': 'createitems',
            'page.delete': 'deleteitems',
            'page.edit': 'updateitems',
            'page.import': 'importitems',
            'page.inherit': 'updateinheritance',
            'page.linkTemplate': 'linktemplates',
            'page.localize': 'createitems',
            'page.publish': 'publishpages',
            'page.unlocalize': 'createitems',
            'page.translate': 'createitems',
            'page.view': 'readitems',
            synchronizeChannel: 'channelsync',
            'template.create': 'createtemplates',
            'template.delete': 'deletetemplates',
            'template.edit': 'updatetemplates',
            'template.inherit': 'updateinheritance',
            'template.link': 'linktemplates',
            'template.localize': 'createtemplates',
            'template.unlocalize': 'deletetemplates',
            'template.view': 'readtemplates',
            'tagType.create': 'createitems',
            'tagType.delete': 'deleteitems',
            'tagType.edit': 'updateitems',
            'tagType.view': 'readitems',
            wastebin: 'wastebin',
        };

        it('returns all-false permissions object when input is null or undefined', () => {
            for (let input of [null, undefined] as any[]) {
                let permissions = mapToPermissions(null, input) as any;
                for (let itemType of Object.keys(permissions)) {
                    for (let prop of Object.keys((permissions )[itemType])) {
                        expect(permissions[itemType][prop]).toBe(false);
                    }
                }
            }
        });

    });

});
