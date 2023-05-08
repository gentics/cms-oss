import { fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { File as FileModel } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { FolderActionsService } from '../../../state';
import { Api } from '../api/api.service';
import { SortedFiles, UploadConflictService } from './upload-conflict.service';

describe('UploadConflictService', () => {

    let uploadConflictService: UploadConflictService;
    let api: MockApi;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                UploadConflictService,
                { provide: Api, useClass: MockApi },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: ModalService, useClass: MockModalService },
            ],
        });
        uploadConflictService = TestBed.get(UploadConflictService);
        api = TestBed.get(Api);
    });

    describe('uploadFilesWithConflictsCheck()', () => {

        let folderActions: FolderActionsService;
        const nodeId = 1;
        const folderId = 2;
        const allFilesToUpload: any[] = [
            mockFile('file1.ext', 'file'),
            mockFile('file2.ext', 'file'),
            mockFile('file3.ext', 'file'),
            mockFile('image1.ext', 'image'),
            mockFile('image2.ext', 'image'),
        ];

        beforeEach(() => {
            folderActions = TestBed.get(FolderActionsService);
        });

        it('should call checkForConflicts(), sortFilesForUpload(), and folderActions.uploadAndReplace() for one file', fakeAsync(() => {
            spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(Promise.resolve([]));
            spyOn(uploadConflictService, 'sortFilesForUpload').and.callThrough();
            spyOn(folderActions, 'uploadAndReplace');
            const filesToUpload = [ allFilesToUpload[0] ];

            uploadConflictService.uploadFilesWithConflictsCheck(filesToUpload, nodeId, folderId);
            tick();

            expect(uploadConflictService.checkForConflicts).toHaveBeenCalledWith(filesToUpload, nodeId, folderId);
            expect(uploadConflictService.sortFilesForUpload).toHaveBeenCalledWith(filesToUpload, []);
            const expectedSortedFiles: SortedFiles = {
                create: { files: filesToUpload, images: [] },
                replace: { files: [], images: [] },
            };
            expect(folderActions.uploadAndReplace).toHaveBeenCalledWith(expectedSortedFiles, folderId, nodeId);
        }));

        it('should call checkForConflicts(), sortFilesForUpload(), and folderActions.uploadAndReplace() for multiple files',
            fakeAsync(() => {
                spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(Promise.resolve([]));
                spyOn(uploadConflictService, 'sortFilesForUpload').and.callThrough();
                spyOn(folderActions, 'uploadAndReplace');
                const filesToUpload: any[] = allFilesToUpload;

                uploadConflictService.uploadFilesWithConflictsCheck(filesToUpload, nodeId, folderId);
                tick();

                expect(uploadConflictService.checkForConflicts).toHaveBeenCalledWith(filesToUpload, nodeId, folderId);
                expect(uploadConflictService.sortFilesForUpload).toHaveBeenCalledWith(filesToUpload, []);

                const expectedSortedFiles: SortedFiles = {
                    create: { files: filesToUpload.slice(0, 3), images: filesToUpload.slice(3) },
                    replace: { files: [], images: [] },
                };
                expect(folderActions.uploadAndReplace).toHaveBeenCalledWith(expectedSortedFiles, folderId, nodeId);
            }));

        it('should handle conflicts correctly', fakeAsync(() => {
            const filesToUpload: any[] = allFilesToUpload;
            let conflictingFiles: any[] = [
                { id: 1, type: 'file', name: 'file1.ext' },
                { id: 3, type: 'image', name: 'image2.ext' },
            ];

            const modalService: ModalService = TestBed.get(ModalService);
            spyOn(uploadConflictService, 'checkForConflicts').and.returnValue(Promise.resolve(conflictingFiles));
            const sortFilesSpy = spyOn(uploadConflictService, 'sortFilesForUpload').and.callThrough();
            spyOn(folderActions, 'uploadAndReplace');

            // Mock a conflict resolution by selecting the first and the last file for replacement
            spyOn(modalService, 'fromComponent').and.callFake(((component: any, options: any, locals: { conflictingFiles: FileModel[] }) => {
                const open = () => Promise.resolve([ ...locals.conflictingFiles ]);
                return Promise.resolve({
                    open,
                });
            }) as any);

            uploadConflictService.uploadFilesWithConflictsCheck(filesToUpload, nodeId, folderId);
            tick();

            expect(uploadConflictService.checkForConflicts).toHaveBeenCalledWith(filesToUpload, nodeId, folderId);
            expect(sortFilesSpy.calls.argsFor(0)[0]).toEqual(filesToUpload);
            expect(sortFilesSpy.calls.argsFor(0)[1]).toEqual(conflictingFiles);

            const expectedSortedFiles: SortedFiles = {
                create: { files: filesToUpload.slice(1, 3), images: [ filesToUpload[3] ] },
                replace: {
                    files: [
                        { id: conflictingFiles[0].id, file: filesToUpload[0] },
                    ],
                    images: [
                        { id: conflictingFiles[1].id, file: filesToUpload[4] },
                    ],
                },
            };
            expect(folderActions.uploadAndReplace).toHaveBeenCalledWith(expectedSortedFiles, folderId, nodeId);
        }));

    });

    describe('checkForConflicts()', () => {

        it('should make an api request for all items in the folder', waitForAsync(() => {
            let spy = spyOn(api.folders, 'getItems').and.callThrough();
            uploadConflictService.checkForConflicts([mockFile('test')], 1, 1)
                .then(() => {
                    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                    expect((spy.calls.argsFor(0) as any[])[2]).toEqual({
                        nodeId: 1,
                        maxItems: -1,
                    });
                });
        }));

        it('should return empty array when no matches', waitForAsync(() => {
            uploadConflictService.checkForConflicts([mockFile('noMatch1'), mockFile('noMatch2')], 1, 1)
                .then(result => {
                    expect(result).toEqual([]);
                });
        }));

        it('should return matching file names', waitForAsync(() => {
            uploadConflictService.checkForConflicts([mockFile('file1.ext'), mockFile('noMatch'), mockFile('file3.ext')], 1, 1)
                .then(result => {
                    expect(result).toEqual([
                        { id: 1, type: 'file', name: 'file1.ext' } as FileModel,
                        { id: 3, type: 'file', name: 'file3.ext' } as FileModel,
                    ]);
                });
        }));

        it('should take into account file type when matching', waitForAsync(() => {
            uploadConflictService.checkForConflicts([mockFile('file2.ext', 'image')], 1, 1)
                .then(result => {
                    expect(result).toEqual([]);
                });
        }));

        it('should replace spaces with underscores when checking file name', waitForAsync(() => {
            uploadConflictService.checkForConflicts([mockFile('_some image with spaces.ext', 'image')], 1, 1)
                .then(result => {
                    expect(result).toEqual([{ id: 2, type: 'image', name: '_some_image_with_spaces.ext' } as any]);
                });
        }));

        it('should replace spaces with dashes when checking file name', waitForAsync(() => {
            uploadConflictService.checkForConflicts([mockFile('some image with dashes.ext', 'image')], 1, 1)
                .then(result => {
                    expect(result).toEqual([{ id: 3, type: 'image', name: 'some-image-with-dashes.ext' } as any]);
                });
        }));
    });

    describe('sortFilesForUpload()', () => {

        const filesToUpload: any[] = [
            mockFile('file1.ext', 'file'),
            mockFile('file2.ext', 'file'),
            mockFile('file3.ext', 'file'),
            mockFile('image1.ext', 'image'),
            mockFile('image2.ext', 'image'),
        ];

        it('should handle undefined itemsToReplace arg', () => {
            let result = uploadConflictService.sortFilesForUpload(filesToUpload, <any> undefined);
            expect(result.create.images).toEqual(filesToUpload.slice(3));
            expect(result.create.files).toEqual(filesToUpload.slice(0, 3));
            expect(result.replace.images).toEqual([]);
            expect(result.replace.files).toEqual([]);
        });

        it('should sort files and images', () => {
            let result = uploadConflictService.sortFilesForUpload(filesToUpload, []);
            expect(result.create.images).toEqual(filesToUpload.slice(3));
            expect(result.create.files).toEqual(filesToUpload.slice(0, 3));
            expect(result.replace.images).toEqual([]);
            expect(result.replace.files).toEqual([]);
        });

        it('should identify files to replace', () => {
            let itemsToReplace: any[] = [
                { id: 1, type: 'file', name: 'file1.ext' },
                { id: 2, type: 'file', name: 'file2.ext' },
            ];
            let result = uploadConflictService.sortFilesForUpload(filesToUpload, itemsToReplace);

            expect(result.create.images).toEqual(filesToUpload.slice(3), 'x');
            expect(result.create.files).toEqual([filesToUpload[2]], 'a');
            expect(result.replace.files).toEqual([
                { id: 1, file: filesToUpload[0] },
                { id: 2, file: filesToUpload[1] },
            ],  'b');
            expect(result.replace.images).toEqual([]);
        });

        it('should identify images to replace', () => {
            let itemsToReplace: any[] = [
                { id: 2, type: 'image', name: 'image2.ext' },
            ];
            let result = uploadConflictService.sortFilesForUpload(filesToUpload, itemsToReplace);

            expect(result.create.images).toEqual([filesToUpload[2]]);
            expect(result.create.files).toEqual(filesToUpload.slice(0, 3));
            expect(result.replace.files).toEqual([]);
            expect(result.replace.images).toEqual([
                { id: 2, file: filesToUpload[4] },
            ]);
        });
    });
});

/**
 * Create a mock File object with just the properties we need for the tests.
 */
function mockFile(name: string, type: 'file' | 'image' = 'file'): File {
    try {
        return new File([''], name, { type: type === 'image' ? 'image/jpeg' : 'text/html' });
    } catch (ignored) {
        const file = Object.create(File.prototype);
        Object.defineProperty(file, 'name', { value: name });
        Object.defineProperty(file, 'type', { value: type === 'image' ? 'image/jpeg' : 'text/html' });
        return file;
    }
}

class MockApi {
    folders = {
        getItems: (): Observable<any> => Observable.of({
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
    uploadAndReplace(): void { }
}

class MockModalService {
    fromComponent(): void { }
}
