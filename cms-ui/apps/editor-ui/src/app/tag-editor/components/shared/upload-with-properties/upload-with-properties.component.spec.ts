import { ChangeDetectorRef, Component } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { BrowseBoxComponent } from '@gentics/cms-components';
import { File as FileModel, Folder } from '@gentics/cms-models';
import { getExampleFileObjectData, getExampleFolderData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { UploadConflictService } from '../../../../core/providers/upload-conflict/upload-conflict.service';
import { FilePropertiesForm } from '../../../../shared/components/file-properties-form/file-properties-form.component';
import { DynamicDisableDirective } from '../../../../shared/directives/dynamic-disable/dynamic-disable.directive';
import { FileSizePipe } from '../../../../shared/pipes/file-size/file-size.pipe';
import { RepositoryBrowserClient } from '../../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { ApplicationStateService, FolderActionsService } from '../../../../state';
import { ImagePreviewComponent } from '../image-preview/image-preview.component';
import { UploadWithPropertiesComponent } from './upload-with-properties.component';

/**
 * TODO: Implement tests after feature release in Dec 2018.
 */
describe('UploadWithPropertiesComponent', () => {
    let folderActions: FolderActionsService;
    let i18nService: I18nService;
    let repositoryBrowserClient: RepositoryBrowserClient;
    let uploadConflictService: UploadConflictService;
    let uploadWithProperties: UploadWithPropertiesComponent;
    let mockFile: File;
    let mockFolder: Folder;
    let modalService: MockModalService;

    beforeEach(() => {
        mockFile = getExampleFileObjectData() as File;
        mockFolder = getExampleFolderData();

        configureComponentTest({
            imports: [
                FormsModule,
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
            ],
            providers: [
                UploadConflictService,
                UploadWithPropertiesComponent,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: GCMSRestClientService, useClass: MockClient },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClientService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: I18nService, useClass: MockI18nService },
                { provide: ChangeDetectorRef, useClass: MockChangeDetectorRef },
                { provide: ModalService, useClass: MockModalService },
            ],
            declarations: [
                BrowseBoxComponent,
                DynamicDisableDirective,
                FilePropertiesForm,
                FileSizePipe,
                ImagePreviewComponent,
                UploadWithPropertiesComponent,
                TestComponent,
            ],
        });

        folderActions = TestBed.get(FolderActionsService);
        i18nService = TestBed.get(I18nService);
        modalService = TestBed.get(ModalService);
        repositoryBrowserClient = TestBed.get(RepositoryBrowserClient);
        uploadConflictService = TestBed.get(UploadConflictService);
        uploadWithProperties = TestBed.get(UploadWithPropertiesComponent);
    });

    describe('upload', () => {
        beforeEach(() => {
            spyOn(uploadWithProperties, 'uploadFileOrImage').and.callThrough();
        });

        it('works for file with conflicting name',
            componentTest(() => TestComponent, () => {
                const conflictingFiles: any[] = [
                    { id: 1, type: 'file', name: 'file1.ext' },
                    { id: 3, type: 'image', name: 'image2.ext' },
                ];
                const checkForConflicts = spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(of(conflictingFiles));
                const uploadSpy = spyOn(uploadConflictService, 'uploadFilesWithConflictsCheck').and.callThrough();
                const modalServiceSpy =
                    spyOn(modalService, 'fromComponent').and.callFake(((component: any, options: any, locals: { conflictingFiles: FileModel[] }) => {
                        const open = () => Promise.resolve([ ...locals.conflictingFiles ]);
                        return Promise.resolve({
                            open,
                        });
                    }) as any);

                uploadWithProperties.uploadFileOrImage(mockFile, mockFolder, {}).subscribe();
                tick();

                expect(uploadSpy).toHaveBeenCalled();
                expect(checkForConflicts).toHaveBeenCalled();
                expect(modalServiceSpy).toHaveBeenCalled();
            }),
        );

        it('works for file without conflicting name',
            componentTest(() => TestComponent, () => {
                const checkForConflicts = spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(of([]));
                const uploadSpy = spyOn(uploadConflictService, 'uploadFilesWithConflictsCheck').and.callThrough();
                const modalServiceSpy =
                    spyOn(modalService, 'fromComponent').and.callFake(((component: any, options: any, locals: { conflictingFiles: FileModel[] }) => {
                        const open = () => Promise.resolve([ ...locals.conflictingFiles ]);
                        return Promise.resolve({
                            open,
                        });
                    }) as any);

                uploadWithProperties.uploadFileOrImage(mockFile, mockFolder, {});
                tick();

                expect(uploadSpy).toHaveBeenCalled();
                expect(checkForConflicts).toHaveBeenCalled();
                expect(modalServiceSpy).not.toHaveBeenCalled();
            }),
        );

        it('file properties are updated after upload',
            componentTest(() => TestComponent, () => {
                uploadWithProperties.itemType = 'file';
                const checkForConflicts = spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(of([]));
                const uploadSpy = spyOn(uploadConflictService, 'uploadFilesWithConflictsCheck').and.callThrough();
                const modalServiceSpy =
                    spyOn(modalService, 'fromComponent').and.callFake(((component: any, options: any, locals: { conflictingFiles: FileModel[] }) => {
                        const open = () => Promise.resolve([ ...locals.conflictingFiles ]);
                        return Promise.resolve({
                            open,
                        });
                    }) as any);
                const updateFilePropertiesSpy = spyOn(folderActions, 'updateFileProperties').and.callThrough();
                const updateImagePropertiesSpy = spyOn(folderActions, 'updateImageProperties').and.callThrough();
                const uploadAndReplaceSpy = spyOn(folderActions, 'uploadAndReplace').and.callThrough();

                uploadWithProperties.uploadFileOrImage(mockFile, mockFolder, { name: 'newName.file', description: 'new description' }).subscribe(() => {});
                tick();

                expect(checkForConflicts).toHaveBeenCalled();
                expect(uploadSpy).toHaveBeenCalled();
                expect(modalServiceSpy).not.toHaveBeenCalled();
                expect(uploadAndReplaceSpy).toHaveBeenCalled();
                expect(updateFilePropertiesSpy).toHaveBeenCalledWith(4, { name: 'newName.file', description: 'new description'}, jasmine.anything());
                expect(updateImagePropertiesSpy).not.toHaveBeenCalled();

            }),
        );

        it('image properties are updated after upload',
            componentTest(() => TestComponent, () => {
                uploadWithProperties.itemType = 'image';
                const checkForConflicts = spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(of([]));
                const uploadSpy = spyOn(uploadConflictService, 'uploadFilesWithConflictsCheck').and.callThrough();
                const modalServiceSpy =
                    spyOn(modalService, 'fromComponent').and.callFake(((component: any, options: any, locals: { conflictingFiles: FileModel[] }) => {
                        const open = () => Promise.resolve([ ...locals.conflictingFiles ]);
                        return Promise.resolve({
                            open,
                        });
                    }) as any);
                const updateFilePropertiesSpy = spyOn(folderActions, 'updateFileProperties').and.callThrough();
                const updateImagePropertiesSpy = spyOn(folderActions, 'updateImageProperties').and.callThrough();
                const uploadAndReplaceSpy = spyOn(folderActions, 'uploadAndReplace').and.callThrough();

                uploadWithProperties.uploadFileOrImage(mockFile, mockFolder, { name: 'newName.file', description: 'new description' } ).subscribe(() => {});
                tick();

                expect(checkForConflicts).toHaveBeenCalled();
                expect(uploadSpy).toHaveBeenCalled();
                expect(modalServiceSpy).not.toHaveBeenCalled();
            }),
        );
    });

    it('setting initial folder works',
        componentTest(() => TestComponent, () => {

        }),
    );

    it('browse for folder works',
        componentTest(() => TestComponent, () => {

        }),
    );

    it('changing properties and uploading file works',
        componentTest(() => TestComponent, () => {

        }),
    );

    it('changing properties and uploading image works',
        componentTest(() => TestComponent, () => {

        }),
    );

});

@Component({
    template: `<upload-with-properties
                    [allowFolderSelection]="true"
                    [destinationFolder]="uploadDestination"
                    [itemType]="itemType"
                    (upload)="onUpload($event)">
                </upload-with-properties>`,
})
class TestComponent {

}

class MockRepositoryBrowserClientService {
    openRepositoryBrowser(): void { }
}

class MockClient {
    folder = {
        items: (): Observable<any> => of({
            items: [
                { id: 1, type: 'file', name: 'file1.ext' },
                { id: 2, type: 'file', name: 'file2.ext' },
                { id: 3, type: 'file', name: 'file3.ext' },
                { id: 1, type: 'image', name: 'file1.ext' },
                { id: 2, type: 'image', name: '_some_image_with_spaces.ext' },
                { id: 3, type: 'image', name: 'some-image-with-dashes.ext' },
            ],
        }),
    };
}

class MockFolderActions {
    updateFileProperties(): Promise<any>  {
        return Promise.resolve({});
    }
    updateImageProperties(): Promise<any>  {
        return Promise.resolve({});
    }
    uploadAndReplace(): Observable<any> {
        return of([[{
            response: {
                file: {
                    id: 4,
                },
            },
        }]]);
    }
}

class MockI18nService {}

class MockChangeDetectorRef {}

class MockModalService {
    fromComponent(): void { }
}
