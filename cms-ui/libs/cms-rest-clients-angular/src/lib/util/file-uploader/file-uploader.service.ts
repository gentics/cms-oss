import { Inject, Injectable, Optional } from '@angular/core';
import { FileUploadResponse } from '@gentics/cms-models';
import { Observable, queue, Subject, Subscription } from 'rxjs';

import { FileUploadObject } from './file-upload-object.class';
import { RequestFactory } from './request.factory';

export interface FileProgress {
    /** A temporary unique ID during upload */
    uid: string;

    /** The name of the uploading file */
    name: string;

    /** Mime type of the uploading file */
    type: string;

    /** The total file size in bytes */
    size: number;

    /** An observable that emits the upload progress in percent */
    progress$: Observable<number>;
}

export interface UploadResponse {
    cancelled: boolean;
    error?: Error;
    name: string;
    response: FileUploadResponse;
    statusCode: number;
    uid: string;
}

export interface UploadProgressReporter {
    /** Emits the total progress of all uploads in percent. */
    progress$: Observable<number>;

    /** Emits when files are added/removed. */
    files$: Observable<FileProgress[]>;

    /** Emits when all uploads are done, successfully or with errors. */
    done$: Observable<UploadResponse[]>;
}

/**
 * Available options to set for the Uploader.
 */
export interface UploaderOptions {
    /** URL to where the files should be uploaded to */
    url: string,

    /** Field-Name in the form-data which will contain the file content */
    fileField: string,

    /** Field-Name in the form-data which will contain the file-name */
    fileNameField?: string,

    /** Additional query-parameters to send when uploading a file */
    parameters?: { [k: string]: string | number },

    /** Additional Headers to send when uploading a file */
    headers?: { name: string, value: string }[],
}

interface QueuedFileUploadObject {
    obj: FileUploadObject;
    file: File;
    request: XMLHttpRequest;
    parameters?: { [k: string]: string | number };
    fileName?: string;
}

/**
 * A service that allows file uploading via XHR2 POST requests, with
 * observable progress and mock support for unit tests.
 * Because angular's Http & NgForm do not support file uploads (yet),
 * uploading via XMLHttpRequest is the best option.
 *
 * Usage:
 *   let uploader = new FileUploader();
 *   uploader.setOptions({ ... });
 *   uploader.upload(file);
 */
@Injectable()
export class FileUploader implements UploadProgressReporter {

    /**
     * An observable that emits the total upload progress in percent.
     */
    public get progress$(): Observable<number> {
        return this.progressEmitter;
    }

    /**
     * An observable that emits when files are added to the uploader or finished uploading.
     */
    public get files$(): Observable<FileProgress[]> {
        return this.filesEmitter;
    }

    public get done$(): Observable<UploadResponse[]> {
        return this.doneEmitter;
    }

    private progressEmitter = new Subject<number>();
    private filesEmitter = new Subject<FileProgress[]>();
    private doneEmitter = new Subject<UploadResponse[]>();

    /** Limit of how many uploads can be done simultaniously */
    private concurrentUploadLimit = 1;
    /** List of files which are queued to be uploaded */
    private queuedList: QueuedFileUploadObject[] = [];
    /** List of files which are being uploaded or which are finished */
    private inProgressList: FileUploadObject[] = [];
    /** List of all files */
    private allFiles: FileUploadObject[] = [];

    /** All opened subscriptions. Used for cleanup on destroy */
    private subscriptions: Subscription[] = [];
    private lastProgressPercent: number;
    /** Request-Factory to create new requests */
    private requests: RequestFactory;
    private destroyed = false;

    /** Upload Options */
    private url: string;
    private fileField: string;
    private fileNameField: string;
    private parameters: { [k: string]: string | number };
    private headers: { name: string, value: string }[];

    constructor(@Inject(RequestFactory) @Optional() requests: RequestFactory) {
        this.requests = requests || new RequestFactory();
    }

    destroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.subscriptions = [];
        this.destroyed = true;
    }

    setOptions(options: UploaderOptions): void {
        this.url = options.url;
        this.fileField = options.fileField;
        this.fileNameField = options.fileNameField;
        this.parameters = options.parameters || {};
        this.headers = options.headers || [];
    }

    upload(file: File, parameters?: { [k: string]: string | number }, fileName?: string): Observable<number> {

        if (this.destroyed) {
            throw new Error('File Uploader already destroyed.');
        } else if (!this.url) {
            throw new Error('No url set, use setOptions() first.');
        }

        let request = this.requests.create();
        const requestUrl = new URL(this.url, window.location.toString());

        request.open('POST', requestUrl, true);
        request.setRequestHeader('Accept', 'application/json');

        this.headers.forEach(header => request.setRequestHeader(header.name, header.value));
        let fileObj = new FileUploadObject(file, request);

        // Add this to all files
        this.allFiles.push(fileObj);
        this.filesEmitter.next(this.allFiles);

        let queuedObj: QueuedFileUploadObject = {
            obj: fileObj,
            file,
            request,
            fileName,
            parameters,
        };

        if (this.canProcessNext()) {
            this.performUpload(queuedObj);
        } else {
            this.queuedList.push(queuedObj);
        }

        return fileObj.progress$;
    }

    /**
     * Retrieve the given FileUploadObject by its uid.
     */
    getFileByUid(uid: string): FileUploadObject {
        const matches = this.inProgressList.filter(f => f.uid === uid);
        if (matches.length === 1) {
            return matches[0];
        }
    }

    private performUpload(fileObj: QueuedFileUploadObject): void {
        let formData = new FormData();
        let params = Object.assign({}, this.parameters, fileObj.parameters);
        Object.keys(params).forEach(key => formData.append(key, String(params[key])));
        let fileName = (fileObj.fileName ? fileObj.fileName : fileObj.file.name);

        if (this.fileNameField) {
            formData.append(this.fileNameField, fileName);
        }
        formData.append(this.fileField, fileObj.file, fileName);

        this.inProgressList.push(fileObj.obj);
        fileObj.request.send(formData);

        let subscription = fileObj.obj.progress$.subscribe(
            () => this.onProgress(),
            error => this.onFileError(error),
            () => this.onDone(),
        );
        this.subscriptions.push(subscription);
    }

    private canProcessNext(): boolean {
        return this.inProgressList.filter(file => !file.done).length < this.concurrentUploadLimit;
    }

    private onProgress(): void {
        let totalProgress = 0;
        let totalSize = 0;
        this.inProgressList.forEach(file => {
            totalProgress += file.uploaded;
            totalSize += file.size;
        });

        if (this.lastProgressPercent !== totalProgress / totalSize) {
            this.progressEmitter.next(this.lastProgressPercent = totalProgress / totalSize);
        }
    }

    private onFileError(err: Error): void {
        console.error(err);
        this.onDone();
    }

    private onDone(): void {
        if (this.queuedList.length > 0 && this.canProcessNext()) {
            this.performUpload(this.queuedList.shift());
            return;
        }

        if (this.inProgressList.every(file => file.done)) {
            let responses = this.inProgressList.map(file => (<UploadResponse> {
                cancelled: file.cancelled,
                error: file.error,
                name: file.name,
                response: file.response,
                statusCode: file.statusCode,
                uid: file.uid,
            }));
            this.doneEmitter.next(responses);
            this.filesEmitter.complete();
            this.progressEmitter.complete();
            this.doneEmitter.complete();
            this.destroy();
        }
    }
}
