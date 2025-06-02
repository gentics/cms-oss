import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import {
    ContentRepositoryType,
    EditableFolderProps,
    Feature,
    Folder,
    FolderListResponse,
    Raw,
    ResponseCode,
} from '@gentics/cms-models';
import { getExampleFolderData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { FormProperties, GenticsUICoreModule } from '@gentics/ui-core';
import { of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { EditableProperties, emptyItemInfo } from '../../../common/models';
import { ApplicationStateService, SetFeatureAction } from '../../../state';
import { MockAppState, TestApplicationState } from '../../../state/test-application-state.mock';
import { DynamicDisableDirective } from '../../directives/dynamic-disable/dynamic-disable.directive';
import { FolderPropertiesComponent, FolderPropertiesMode } from './folder-properties.component';

function getInput<T extends keyof EditableFolderProps>(
    fixture: ComponentFixture<TestComponent>,
    controlName: T,
): FormProperties<EditableFolderProps>[T] | null {
    return fixture.componentInstance?.form?.form?.controls?.[controlName];
}

function triggerInputEvent(element: HTMLElement): void {
    const customEvent: Event = document.createEvent('Event');
    customEvent.initEvent('input', false, false);
    element.dispatchEvent(customEvent);
}

function setInputValue<K extends keyof EditableFolderProps, T = EditableFolderProps[K]>(
    fixture: ComponentFixture<TestComponent>,
    formcontrolname: K, value: T,
): void {
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
    currentNodeId: number,
    currentNodePubDirSegment: boolean,
    contentRepositoryId: number,
    currentFolderId: number,
    testFolder: Folder<Raw>,
    contentRepositoryType: ContentRepositoryType,
): void {
    // test component instance
    instance.nodeId = currentNodeId;
    instance.folderId = currentFolderId;
    instance.properties = {
        name: testFolder.name,
        publishDir: testFolder.publishDir,
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
        <gtx-folder-properties
            #form
            [nodeId]="nodeId"
            [folderId]="folderId"
            [value]="properties"
            [disabled]="disabled"
            [mode]="mode"
            (valueChange)="simplePropertiesChanged($event)"
        ></gtx-folder-properties>
    `,
    standalone: false,
})
class TestComponent {
    @ViewChild('form', { static: true })
    form: FolderPropertiesComponent;

    nodeId: number;
    folderId: number;
    properties: EditableProperties;
    simplePropertiesChanged = jasmine.createSpy('simplePropertiesChanged');
    mode: FolderPropertiesMode = FolderPropertiesMode.CREATE;
    disabled = false;
}

function createFolderListResponse(folders: Folder[] = []): FolderListResponse {
    return {
        folders,
        hasMoreItems : false,
        messages: [],
        numItems: folders.length,
        responseInfo: {
            responseCode: ResponseCode.OK,
            responseMessage: 'Successfully loaded subfolders',
        },
    };
}

describe('FolderPropertiesComponent', () => {
    let state: TestApplicationState;
    let initialState: MockAppState;
    let client: GCMSRestClientService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                ReactiveFormsModule,
                GenticsUICoreModule,
                GCMSRestClientModule,
            ],
            declarations: [
                DynamicDisableDirective,
                FolderPropertiesComponent,
                TestComponent,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
            ],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        client = TestBed.inject(GCMSRestClientService);

        spyOn(client.folder, 'folders').and.callFake(() => of(createFolderListResponse()));
        spyOn(client.folder, 'get').and.callFake(() => of({
            folder: getExampleFolderData({ id: 1, userId: 3, publishDir: ACTIVE_FOLDER_PUBLISH_DIR}),
            messages: [],
            responseInfo: {
                responseCode: ResponseCode.OK,
                responseMessage: 'Successfully loaded subfolders',
            },
        }));
        spyOn(client.folder, 'sanitizePublshDirectory').and.callFake(() => of({
            messages: [],
            responseInfo: {
                responseCode: ResponseCode.OK,
                responseMessage: 'Successfully loaded subfolders',
            },
            publishDir: SANITIZATION_RESULT,
        }));

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

                instance.mode = FolderPropertiesMode.CREATE;
                instance.properties = {
                    name: '',
                    publishDir: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'publishDir').value).toEqual(PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                });
                expect(client.folder.sanitizePublshDirectory).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));

                instance.mode = FolderPropertiesMode.CREATE;
                instance.properties = {
                    name: '',
                    publishDir: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'publishDir').value).toEqual(SANITIZATION_RESULT);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: SANITIZATION_RESULT,
                    description: '',
                });
                expect((client.folder.sanitizePublshDirectory as jasmine.Spy).calls.mostRecent().args[0]).toEqual({ nodeId: ACTIVE_NODE_ID, publishDir: `${ACTIVE_FOLDER_PUBLISH_DIR}${TEST_STRING_FOLDER_NAME}` });
            }),
        );

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = FolderPropertiesMode.CREATE;
                instance.properties = {
                    name: '',
                    publishDir: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'publishDir').value).toEqual(PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                });
                expect(client.folder.sanitizePublshDirectory).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));
                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = FolderPropertiesMode.CREATE;
                instance.properties = {
                    name: '',
                    publishDir: PSEUDO_EMPTY_DEFAULT_DIRECTORY_NAME_TO_BYPASS_REQUIRED_VALIDATOR,
                    description: '',
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual('');

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);
                expect(getInput(fixture, 'publishDir').value).toEqual(SANITIZATION_RESULT);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: SANITIZATION_RESULT,
                    description: '',
                });
                expect((client.folder.sanitizePublshDirectory as jasmine.Spy).calls.mostRecent().args[0]).toEqual({ nodeId: ACTIVE_NODE_ID, publishDir: `${TEST_STRING_FOLDER_NAME}` });
            }),
        );

    });

    describe('behaves correctly in mode EDIT', () => {

        const TEST_STRING_FOLDER_NAME_NEW = 'xxxxx';

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = '/custom_publish_dir/';

                instance.mode = FolderPropertiesMode.EDIT;
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: undefined,
                    nameI18n: undefined,
                    publishDirI18n: undefined,
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME_NEW);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME_NEW);
                expect(getInput(fixture, 'publishDir').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(client.folder.sanitizePublshDirectory).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = FALSE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = '/custom_publish_dir/';

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));

                instance.mode = FolderPropertiesMode.EDIT;
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
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
                expect(getInput(fixture, 'publishDir').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(client.folder.sanitizePublshDirectory).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = FALSE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = 'testPubDirSegmentPath';

                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = FolderPropertiesMode.EDIT;
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
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
                expect(getInput(fixture, 'publishDir').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(client.folder.sanitizePublshDirectory).not.toHaveBeenCalled();
            }),
        );

        it('with feature setting autocomplete_folder_path = TRUE and pub_dir_segment = TRUE',
            componentTest(() => TestComponent, (fixture, instance) => {

                const TEST_STRING_DIRECTORY_NAME = 'testPubDirSegmentPath';

                state.dispatch(new SetFeatureAction(Feature.AUTOCOMPLETE_FOLDER_PATH, true));
                state.dispatch(new SetFeatureAction(Feature.PUB_DIR_SEGMENT, true));
                setPubDirSegmentToTrueInInitialState(state);

                instance.mode = FolderPropertiesMode.EDIT;
                instance.properties = {
                    name: TEST_STRING_FOLDER_NAME,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: undefined,
                    nameI18n: undefined,
                    publishDirI18n: undefined,
                };
                fixture.detectChanges();

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME);

                setInputValue(fixture, 'name', TEST_STRING_FOLDER_NAME_NEW);

                expect(getInput(fixture, 'name').value).toEqual(TEST_STRING_FOLDER_NAME_NEW);
                expect(getInput(fixture, 'publishDir').value).toEqual(TEST_STRING_DIRECTORY_NAME);
                expect(instance.simplePropertiesChanged).toHaveBeenCalledWith({
                    name: TEST_STRING_FOLDER_NAME_NEW,
                    publishDir: TEST_STRING_DIRECTORY_NAME,
                    description: '',
                    descriptionI18n: {},
                    nameI18n: {},
                    publishDirI18n: {},
                });
                expect(client.folder.sanitizePublshDirectory).not.toHaveBeenCalled();
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
                currentNodeId,
                currentNodePubDirSegment,
                contentRepositoryId,
                currentFolderId,
                testFolder,
                contentRepositoryType,
            );
            (client.folder.folders as jasmine.Spy).and.callFake(() => of(createFolderListResponse(currentFolderSubFolders)));

            tick(1_000);
            fixture.detectChanges();
            tick(1_000);

            setInputValue(fixture, 'name', '');
            setInputValue(fixture, 'publishDir', '');

            tick(1_000);
            fixture.detectChanges();
            tick(1_000);

            // check if form input values are equal to data provided via parent component input
            expect(getInput(fixture, 'name').value).toBe('');
            expect(getInput(fixture, 'name').valid).toBe(false);

            expect(getInput(fixture, 'publishDir').value).toBe('');
            expect(getInput(fixture, 'publishDir').valid).toBe(false);

            expect(getInput(fixture, 'description').value).toBe(testFolder.description);
            expect(getInput(fixture, 'description').valid).toBe(true);
        }),
    );

});
