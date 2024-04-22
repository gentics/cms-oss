import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import {
    EditMode,
    File as FileModel,
    Folder,
    FolderItemType,
    Image,
    Node,
    Page,
} from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { of } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { RepositoryBrowserClient } from '../../../shared/providers';
import { ApplicationStateService, FolderActionsService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { CustomScriptHostService } from './custom-script-host.service';

let filePickerButton: { click: jasmine.Spy };

describe('CustomScriptHostService', () => {

    let mockContentFrame: MockContentFrame;
    let customScriptHostService: CustomScriptHostService;
    let appState: TestApplicationState;
    let resourceUrlBuilder: MockResourceUrlBuilder;
    let folderActions: MockFolderActions;
    let navigationService: MockNavigationService;
    let router: Router;
    let repositoryBrowserClient: RepositoryBrowserClient;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                RouterTestingModule.withRoutes([]),
            ],
            providers: [
                CustomScriptHostService,
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: I18nService, useClass: MockI18nService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: ResourceUrlBuilder, useClass: MockResourceUrlBuilder },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClientService},
            ],
        });

        mockContentFrame = new MockContentFrame();
        customScriptHostService = TestBed.get(CustomScriptHostService);
        appState = TestBed.get(ApplicationStateService);
        resourceUrlBuilder = TestBed.get(ResourceUrlBuilder);
        folderActions = TestBed.get(FolderActionsService);
        navigationService = TestBed.get(NavigationService);
        router = TestBed.get(Router);
        repositoryBrowserClient = TestBed.get(RepositoryBrowserClient);

        customScriptHostService.initialize(mockContentFrame as any);
        filePickerButton = {
            click: jasmine.createSpy('filePickerButtonClick'),
        };
    });

    it('setRequesting() sets the value of ContentFrame.requesting & runs change detection', () => {
        customScriptHostService.setRequesting(true);

        expect(mockContentFrame.requesting).toBe(true);
        expect(mockContentFrame.runChangeDetection).toHaveBeenCalled();
    });

    it('setContentModified() delegates to ContentFrame.setContentModified() with second arg true', () => {
        customScriptHostService.setContentModified(false);

        expect(mockContentFrame.setContentModified).toHaveBeenCalledWith(false, true);
    });

    it('setObjectPropertyModified() updates state and runs change detection', () => {
        customScriptHostService.setObjectPropertyModified(true);

        expect(mockContentFrame.runChangeDetection).toHaveBeenCalled();
        const currentState = appState.now.editor;
        expect(currentState.objectPropertiesModified).toBeTrue();
        expect(currentState.modifiedObjectPropertiesValid).toBeTrue();
    });

    describe('openFilePicker()', () => {

        it('sets the correct properties on the FilePicker instance', () => {
            customScriptHostService.openFilePicker('image');

            expect(mockContentFrame.filePicker.multiple).toBe(false);
            expect(mockContentFrame.filePicker.accept).toBe('image/*');

            customScriptHostService.openFilePicker('file');
            expect(mockContentFrame.filePicker.multiple).toBe(false);
            expect(mockContentFrame.filePicker.accept).toBe('*');
        });

        it('triggers a click on the file picker button', () => {
            customScriptHostService.openFilePicker('image');

            expect(filePickerButton.click).toHaveBeenCalled();
        });

        it('returns an observable of the FilePicker fileSelect stream', done => {
            const result = customScriptHostService.openFilePicker('image');

            result.subscribe(
                value => {
                    expect(value).toBe('MOCK_FILE' as any);
                },
                err => { },
                () => {
                    // ensure the stream completes
                    done();
                });
        });
    });

    describe('uploadForCurrentItem()', () => {

        let uploadResponses: any[] = [];

        beforeEach(() => {
            uploadResponses = [
                { response: { file: { foo: 'bar' } } },
                { response: { file: { baz: 'quux' } } },
            ];
            folderActions.uploadFiles = jasmine.createSpy('uploadFiles')
                .and.returnValue(of(uploadResponses));
        });

        it('calls FolderActionsService.uploadFiles() with correct args for image when node has a defaultImageFolderId', () => {
            mockContentFrame.currentNode = {
                defaultImageFolderId: 123,
            } as any;
            const files: any[] = [{}];

            customScriptHostService.uploadForCurrentItem('image', files);

            expect(folderActions.uploadFiles).toHaveBeenCalledWith('image', files, 123);
        });

        it('calls FolderActionsService.uploadFiles() with correct args for image when node has no defaultImageFolderId', () => {
            mockContentFrame.currentNode = {} as any;
            mockContentFrame.currentItem = {
                folderId: 999,
            } as any;
            const files: any[] = [{}];

            customScriptHostService.uploadForCurrentItem('image', files);

            expect(folderActions.uploadFiles).toHaveBeenCalledWith('image', files, 999);
        });

        it('calls FolderActionsService.uploadFiles() with correct args for file when node has a defaultFileFolderId', () => {
            mockContentFrame.currentNode = {
                defaultFileFolderId: 545,
            } as any;
            const files: any[] = [{}];

            customScriptHostService.uploadForCurrentItem('file', files);

            expect(folderActions.uploadFiles).toHaveBeenCalledWith('file', files, 545);
        });

        it('calls FolderActionsService.uploadFiles() with correct args for file when node has no defaultFileFolderId', () => {
            mockContentFrame.currentNode = {} as any;
            mockContentFrame.currentItem = {
                folderId: 554,
            } as any;
            const files: any[] = [{}];

            customScriptHostService.uploadForCurrentItem('file', files);

            expect(folderActions.uploadFiles).toHaveBeenCalledWith('file', files, 554);
        });

        it('returns an observable of the inner file objects from the uploadFiles response', () => {
            mockContentFrame.currentNode = {
                defaultFileFolderId: 545,
            } as any;
            const files: any[] = [{}];

            customScriptHostService.uploadForCurrentItem('file', files)
                .subscribe(result => {
                    expect(result).toEqual([uploadResponses[0].response.file, uploadResponses[1].response.file]);
                });
        });
    });

    describe('openRepositoryBrowser()', () => {

        const SELECTED_ITEMS = [{ foo: 'bar' }];

        beforeEach(() => {
            repositoryBrowserClient.openRepositoryBrowser = jasmine.createSpy('openRepositoryBrowser')
                .and.returnValue(Promise.resolve(SELECTED_ITEMS));
        });

        it('calls repositoryBrowserClient.openRepositoryBrowser() with correct arguments', () => {
            const options: RepositoryBrowserOptions = { selectMultiple: true, allowedSelection: 'page' };
            const callback = jasmine.createSpy('callback');
            customScriptHostService.openRepositoryBrowser(options, callback);

            expect(repositoryBrowserClient.openRepositoryBrowser).toHaveBeenCalledWith(options);
        });

        it('invokes the callback with the selected items once the modal resolves', done => {
            const options: RepositoryBrowserOptions = { selectMultiple: true, allowedSelection: 'page' };
            const callback = jasmine.createSpy('callback').and.callFake(() => {
                expect(callback).toHaveBeenCalledWith(SELECTED_ITEMS);
                done();
            });
            customScriptHostService.openRepositoryBrowser(options, callback);
        });

    });

    it('navigateToPagePreview() calls correct NavigationService methods', () => {
        const navigateSpy = jasmine.createSpy('navigate');
        const detailSpy = jasmine.createSpy('detail').and.returnValue({
            navigate: navigateSpy,
        });
        const detailOrModalSpy = jasmine.createSpy('detailOrModal').and.callFake(
            (nodeId: number,
                itemType: FolderItemType | 'node' | 'channel',
                itemId: number,
                editMode: EditMode) => detailSpy(nodeId, itemType, itemId, editMode),
        );
        navigationService.detailOrModal = detailOrModalSpy;
        navigationService.detail = detailSpy;
        customScriptHostService.navigateToPagePreview(1, 2);

        expect(detailOrModalSpy).toHaveBeenCalledWith(1, 'page', 2, 'preview');
        expect(detailSpy).toHaveBeenCalledWith(1, 'page', 2, 'preview');
        expect(navigateSpy).toHaveBeenCalled();
    });

    it('getInternalLinkUrlToPagePreview() returns correct url', () => {
        // override the Router.url property with a test value
        Object.defineProperty(router, 'url', {
            get: (): string => '/editor/(detail:node/1/page/16/preview;options=e30%3D//list:node/1/folder/4)',
        });

        const result = customScriptHostService.getInternalLinkUrlToPagePreview(20, 89);
        expect(result)
            .toBe(window.location.pathname + '#/editor/(detail:node/20/page/89/preview;options=e30%3D//list:node/1/folder/4)');
    });

});

class MockContentFrame {
    currentNode: Node;
    currentItem: Page | FileModel | Folder | Image | Node;
    requesting = false;
    runChangeDetection = jasmine.createSpy('runChangeDetection');
    setContentModified = jasmine.createSpy('setContentModified');
    filePicker = {
        multiple: true,
        accept: '',
        fileSelect: of('MOCK_FILE'),
    };
    filePickerWrapper = {
        nativeElement: {
            querySelector: (): any => filePickerButton,
        },
    };
}

class MockFolderActions {
    uploadFiles = jasmine.createSpy('uploadFiles');
}

class MockNavigationService {
    detail = jasmine.createSpy('detail');
    detailOrModal = jasmine.createSpy('detailOrModal');
    modal = jasmine.createSpy('modal');
}

class MockRepositoryBrowserClientService {
    openRepositoryBrowser = jasmine.createSpy('openRepositoryBrowser');
}

class MockEntityResolver { }
class MockI18nNotification { }
class MockI18nService { }
class MockErrorHandler { }
class MockResourceUrlBuilder { }
