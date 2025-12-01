import { Injectable } from '@angular/core';
import { File as FileModel } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService } from '@gentics/ui-core';
import { Observable, from, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { UploadResponse } from '../../../common/models';
import { FolderActionsService } from '../../../state';
import { FileNameConflictModal } from '../../components/file-name-conflict-modal/file-name-conflict-modal.component';

export type FileToReplace = {
    id: number;
    file: File
};

export type SortedFiles = {
    create: { files: File[]; images: File[] };
    replace: { files: FileToReplace[]; images: FileToReplace[] };
};

/**
 * This service provides some methods for dealing with file name conflicts which may arise when uploading
 * binaries to a folder.
 */
@Injectable()
export class UploadConflictService {

    constructor(
        private modalService: ModalService,
        private folderActions: FolderActionsService,
        private client: GCMSRestClientService,
    ) { }

    /**
     * Uploads the specified files to the specified folder by first doing a conflicts check,
     * prompting the user for a conflict resolution if necessary, and then uploading the files.
     *
     * @param filesToUpload the files to be uploaded
     * @param nodeId the ID of the destination node
     * @param folderId the ID of the destination folder
     */
    uploadFilesWithConflictsCheck(filesToUpload: File[], nodeId: number, folderId: number): Observable<UploadResponse[]> {
        const toReplace: Observable<FileModel[]> = this.checkForConflicts(filesToUpload, nodeId, folderId).pipe(
            switchMap(conflictingFiles => {
                if (conflictingFiles.length < 1) {
                    return of([]);
                }

                // File name conflicts - prompt user how to proceed.
                return from(this.modalService.fromComponent(FileNameConflictModal, {}, {
                    conflictingFiles,
                    totalFileCount: filesToUpload.length,
                }).then(modal => modal.open()));
            }),
        );

        return toReplace.pipe(
            switchMap((itemsToReplace: FileModel[]) => {
                const sortedFiles = this.sortFilesForUpload(filesToUpload, itemsToReplace);
                return this.folderActions.uploadAndReplace(sortedFiles, folderId, nodeId);
            }),
        );
    }

    /**
     * Checks the current folder for the existence of file or image objects with names matching any
     * of those in the `filenames` array.
     * Used when uploading files to allow the user to decide whether to
     * a) create a new file object or
     * b) replace the binary associated with the existing file object.
     */
    checkForConflicts(filesToUpload: File[], nodeId: number, folderId: number): Observable<FileModel[]> {
        const isImage = (file: File) => file.type.startsWith('image/');
        const files = filesToUpload.map(rawFile => {
            return {
                name: rawFile.name,
                type: isImage(rawFile) ? 'image' : 'file',
            };
        });

        return this.client.folder.items(folderId, { type: ['file', 'image'], nodeId, maxItems: -1 }).pipe(
            map(response => {
                const fileObjects = response.items as FileModel[];
                return fileObjects.filter(file => this.matchesNameAndType(files, file));
            }),
        );
    }

    /**
     * Given a list of File object to be uploaded, and a list of 'file' items which are to be replaced, this
     * method returns an object describing which of those Files should be newly created, and which are to replace
     * existing file binaries.
     */
    sortFilesForUpload(filesToUpload: File[], itemsToReplace: FileModel[] = []): SortedFiles {
        const sortedFiles: SortedFiles = {
            create: { files: [], images: [] },
            replace: { files: [], images: [] },
        };

        const getReplacementItem = (file: File): FileModel => {
            const matches = itemsToReplace.filter(item => {
                return this.matchesName(file, item) && this.getType(file) === item.type;
            });
            return 0 < matches.length ? matches[0] : null;
        };

        filesToUpload.forEach(file => {
            const type = this.getType(file);
            const replacementItem = getReplacementItem(file);
            if (type === 'image') {
                if (replacementItem) {
                    sortedFiles.replace.images.push({ id: replacementItem.id, file });
                } else {
                    sortedFiles.create.images.push(file);
                }
            } else {
                if (replacementItem) {
                    sortedFiles.replace.files.push({ id: replacementItem.id, file });
                } else {
                    sortedFiles.create.files.push(file);
                }
            }
        });

        return sortedFiles;
    }

    /**
     * Checks to see if the fileObject matches any of name/type pairs provided by the user.
     */
    private matchesNameAndType(files: { name: string, type: string }[], fileOrItem: File | FileModel): boolean {
        const targetType = this.getType(fileOrItem);
        const filtered = files.filter(file => this.matchesName(file, fileOrItem) && file.type === targetType);
        return 0 < filtered.length;
    }

    /**
     * Returns true if the two items have matching name properties, or if the names match when spaces are replaced by
     * underscores.
     */
    private matchesName(item1: { name: string; }, item2: { name: string }): boolean {
        const underscored = (name: string): string => name.replace(/\s/g, '_');
        const dashed = (name: string): string => name.replace(/\s/g, '-');
        return item1.name === item2.name ||
            underscored(item1.name) === underscored(item2.name) ||
            dashed(item2.name) === dashed(item1.name);
    }

    // This should be completely removed, since the server should determine what to do with
    // the mime-type, especially since it's unreliable.
    private getType(fileOrItem: File | FileModel): 'file' | 'image' {
        if (
            fileOrItem instanceof File
            // This is only present for cypress tests, due to conflicting Type-defintions
            || Object.getPrototypeOf(fileOrItem).constructor.name === 'File'
        ) {
            return fileOrItem.type.startsWith('image/') ? 'image' : 'file';
        } else {
            return fileOrItem.type;
        }
    }

}
