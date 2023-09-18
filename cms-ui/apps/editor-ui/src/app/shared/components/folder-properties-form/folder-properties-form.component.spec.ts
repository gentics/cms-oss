import { Component } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import {
    ContentRepositoryType,
    Feature,
    Folder,
    FolderListResponse,
    FolderPublishDirSanitizeResponse,
    FolderResponse,
    GcmsTestData,
    Raw,
} from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { getExampleFolderData } from '@gentics/cms-models/testing/test-data.mock';
import { emptyItemInfo } from '../../../common/models';
import { EditableProperties } from '../../../content-frame/components/properties-editor/properties-editor.component';
import { Api } from '../../../core/providers/api/api.service';
import { ApplicationStateService, SetFeatureAction } from '../../../state';
import { MockAppState, TestApplicationState } from '../../../state/test-application-state.mock';
import { DynamicDisableDirective } from '../../directives/dynamic-disable/dynamic-disable.directive';
import { FolderPropertiesForm } from './folder-properties-form.component';

function getInput<T>(fixture: ComponentFixture<T>, formcontrolname: string): any {
    const input = fixture.nativeElement.querySelector(`[formcontrolname=${formcontrolname}] input`);
    const throwNotDetectableError = (formcontrolname: string) => {
        throw new Error(`Element valid state not detectable identfied via formcontrolname "${formcontrolname}"`);
    };
    const getValidState = (input: HTMLInputElement): boolean => {
        if (!input.parentElement.classList) {
            throwNotDetectableError(formcontrolname);
        }
        switch (input.parentElement.classList.contains('ng-invalid')) {
            case true:
                return false;
            case false:
                return true;
            default:
                throwNotDetectableError(formcontrolname);
        }
    };
    if (!input || !input.attributes) {
        throw new Error(`Element not found identified via formcontrolname "${formcontrolname}"`);
    }
    return {
        value: input.value,
        valid: getValidState(input),
    };
}

function triggerInputEvent(element: HTMLElement): void {
    const customEvent: Event = document.createEvent('Event');
    customEvent.initEvent('input', false, false);
    element.dispatchEvent(customEvent);
}

function setInputValue<T>(fixture: ComponentFixture<T>, formcontrolname: string, value: string): void {
    const input = fixture.nativeElement.querySelector(`[formcontrolname=${formcontrolname}] input`);
    input.value = value;
    triggerInputEvent(input);
    tick();
    fixture.detectChanges();
    tick(400); // debounce
}

function setPubDirSegmentToTrueInInitialState(state: TestApplicationState) {
    state.mockState({
        entities: {
            node: {
                [ACTIVE_NODE_ID]: {
                    id: ACTIVE_NODE_ID,
                    publishDir: NODE_PUBLISH_DIR,
                    pubDirSegment: true,
                },
            },
        },
    });
}

function configureEnvironment<T extends TestComponent>(
    instance: T,
    state: TestApplicationState,
    api: MockApiService,
    currentNodeId: number,
    currentNodePubDirSegment: boolean,
    contentRepositoryId: number,
    currentFolderId: number,
    testFolder: Folder<Raw>,
    currentFolderSubFolders: Folder<Raw>[],
    contentRepositoryType: ContentRepositoryType,
): void {
    // test component instance
    instance.nodeId = currentNodeId;
    instance.folderId = currentFolderId;
    instance.properties = {
        name: testFolder.name,
        directory: testFolder.publishDir,
        description: testFolder.description,
    };

    // state
    state.mockState({
        entities: {
            contentRepository: {
                [contentRepositoryId]: {
                    id: contentRepositoryId,
                    crType: contentRepositoryType,
                },
            },
            node: {
                [currentNodeId]: {
                    id: currentNodeId,
                    contentRepositoryId,
                    pubDirSegment: currentNodePubDirSegment,
                },
            },
        },
        folder: {
            activeNode: currentNodeId,
            activeFolder: currentFolderId,
        },
    });

    // API
    api.folderContent.folders = currentFolderSubFolders;
}

const ACTIVE_FOLDER_ID = 1;
const ACTIVE_FOLDER_PUBLISH_DIR = '/A_new_folder/'
const ACTIVE_NODE_ID = 2;
const NODE_PUBLISH_DIR = '/stuff';

const PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR = 'e';

const TEST_STRING_FOLDER_NAME = 'myNewFolderName';

const SANITIZATION_RESULT = 'sanitizationResult';

@Component({
    template: `
        <folder-properties-form
            [nodeId]="nodeId"
            [folderId]="folderId"
            [properties]="properties"
            [disabled]="disabled"
            [mode]="mode"
            (changes)="simplePropertiesChanged($event)"
        >
        </folder-properties-form>
    `,
    })
class TestComponent {
    nodeId: number;
    folderId: number;
    properties: EditableProperties;
    simplePropertiesChanged = jasmine.createSpy('simplePropertiesChanged');
    mode: 'create' | 'edit' = 'create';
    disabled = false;
}

class MockApiService {

    folderContent: FolderListResponse = {
        folders: [
            // to be filled with test data
        ],
        hasMoreItems : false,
        messages: [],
        numItems: 3,
        responseInfo: {
            responseCode: 'OK',
            responseMessage: 'Successfully loaded subfolders',
        },
    };

    private folder: FolderResponse = {
        folder: GcmsTestData.getExampleFolderData({ id: 1, userId: 3, publishDir: ACTIVE_FOLDER_PUBLISH_DIR}),
        messages: [],
        responseInfo: {
            responseCode: 'OK',
            responseMessage: 'Successfully loaded subfolders',
        },
    }

    private folderPublishDirSanitize: FolderPublishDirSanitizeResponse = {
        messages: [],
        responseInfo: {
            responseCode: 'OK',
            responseMessage: 'Successfully loaded subfolders',
        },
        publishDir: SANITIZATION_RESULT,
    }

    // constructor() {
    //     spyOn(this, 'folders');
    // }

    folders = {
        getFolders: (folderId: number): Observable<FolderListResponse> => {
            return of(this.folderContent);
        },
        getItem: (folderId: number, type: 'folder'): Observable<FolderResponse> => {
            return of(this.folder);
        },
        sanitizeFolderPath: jasmine.createSpy('sanitizeFolderPath').and.returnValue(of(this.folderPublishDirSanitize)),
    };
}


describe('FolderPropertiesForm', () => {
    let state: TestApplicationState;
    let initialState: MockAppState;
    let folder: Folder;
    let api: MockApiService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                ReactiveFormsModule,
                GenticsUICoreModule,
            ],
            declarations: [
                DynamicDisableDirective,
                FolderPropertiesForm,
                TestComponent,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Api, useClass: MockApiService },
            ],
        });

        state = TestBed.get(ApplicationStateService);
        api = TestBed.get(Api);
        folder = getExampleFolderData();
        initialState = {
            auth: {
                isLoggedIn: true,
            },
            features: {
                autocomplete_folder_path: false,
                pub_dir_segment: false,
                nodeFeatures: {},
            },
            entities: {
                node: {
                    [ACTIVE_NODE_ID]: {
                        id: ACTIVE_NODE_ID,
                        publishDir: NODE_PUBLISH_DIR,
                    },
                },
            },
            folder: {
                activeLanguage: 1,
                activeNode: ACTIVE_NODE_ID,
                activeFolder: ACTIVE_FOLDER_ID,
                searchFilters: {
                    nodeId: [ { value: ACTIVE_NODE_ID, operator: 'IS' } ],
                },
                folders: emptyItemInfo,
            },
        };
        state.mockState(initialState);
    });

    describe('behaves correctly in mode CREATE', () => {

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                instance.mode = 'create';
                instance.properties = {
                    name: '',
                    directory: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'directory').value).toEqual(PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    directory: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                });
                expect(api.folders.sanitizeFolderPath).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));

                instance.mode = 'create';
                instance.properties = {
                    name: '',
                    directory: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'directory').value).toEqual(SANITIZATION_RESULT);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    directory: SANITIZATION_RESULT,
                    description: '',
                });
                expect(api.folders.sanitizeFolderPath.calls.mostRecent().args[0]).toEqual({ nodeId: ACTIVE_NODE_ID, publishDir: `${ACTIVE_FOLDER_PUBLISH_DIR}${TEST_STRING_FOLDER_NAME}` });
            }),
        );

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = 'create';
                instance.properties = {
                    name: '',
                    directory: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'directory').value).toEqual(PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    directory: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                });
                expect(api.folders.sanitizeFolderPath).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));
                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = 'create';
                instance.properties = {
                    name: '',
                    directory: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'directory').value).toEqual(SANITIZATION_RESULT);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    directory: SANITIZATION_RESULT,
                    description: '',
                });
                expect(api.folders.sanitizeFolderPath.calls.mostRecent().args[0]).toEqual({ nodeId: ACTIVE_NODE_ID, publishDir: `${TEST_STRING_FOLDER_NAME}` });
            }),
        );

    });

    describe('behaves correctly in mode EDIT', () => {

        const TEST_STRING_FOLDER_NAME_NEW = 'xxxxx';

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = '/custom_publish_dir/';

                instance.mode = 'edit';
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: undefined,
                    nameI18n: undefined,
                    publishDirI18n: undefined,
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME_NEW);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME_NEW);
                expect(getInput(fixture, 'directory').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(api.folders.sanitizeFolderPath).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = '/custom_publish_dir/';

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));

                instance.mode = 'edit';
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: undefined,
                    nameI18n: undefined,
                    publishDirI18n: undefined,
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME_NEW);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME_NEW);
                // autocompletion has no effect in edit mode
                expect(getInput(fixture, 'directory').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(api.folders.sanitizeFolderPath).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = 'testPubDirSegmentPath';

                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = 'edit';
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: undefined,
                    nameI18n: undefined,
                    publishDirI18n: undefined,
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME_NEW);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME_NEW);
                // autocompletion has no effect in edit mode
                expect(getInput(fixture, 'directory').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(api.folders.sanitizeFolderPath).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = 'testPubDirSegmentPath';

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));
                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = 'edit';
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: undefined,
                    nameI18n: undefined,
                    publishDirI18n: undefined,
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME_NEW);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME_NEW);
                expect(getInput(fixture, 'directory').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    directory: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(api.folders.sanitizeFolderPath).not.toHaveBeenCalled();
            }),
        );

    });

    it('required validators work',
        componentTest(() => TestComponent, (fixture, instance) => {

            // assemble test data
            const currentNodeId = 1;
            const currentNodePubDirSegment = false;
            const contentRepositoryId = 1;
            const currentFolderId = 1;
            const testFolder = { ...getExampleFolderData( { id: 2 } ), motherId: currentFolderId, name: 'test-name-02' };
            const currentFolderSubFolders = [
                testFolder,
                { ...getExampleFolderData( { id: 3 } ), motherId: currentFolderId, name: 'test-name-03' },
                { ...getExampleFolderData( { id: 4 } ), motherId: currentFolderId, name: 'test-name-04' },
            ];
            const contentRepositoryType: ContentRepositoryType = ContentRepositoryType.CR;

            // apply test data
            configureEnvironment(
                instance,
                state,
                api,
                currentNodeId,
                currentNodePubDirSegment,
                contentRepositoryId,
                currentFolderId,
                testFolder,
                currentFolderSubFolders,
                contentRepositoryType,
            );

            fixture.detectChanges();

            setInputValue(fixture, 'name', '');
            setInputValue(fixture, 'directory', '');

            // check if form input values are equal to data provided via parent component input
            expect(getInput(fixture, 'name').value).toBe('');
            expect(getInput(fixture, 'name').valid).toBe(false);

            expect(getInput(fixture, 'directory').value).toBe('');
            expect(getInput(fixture, 'directory').valid).toBe(false);

            expect(getInput(fixture, 'description').value).toBe(testFolder.description);
            expect(getInput(fixture, 'description').valid).toBe(true);

            // remove the changes made to the API mock ...
            api.folderContent.folders = [];
        }),
    );

});
