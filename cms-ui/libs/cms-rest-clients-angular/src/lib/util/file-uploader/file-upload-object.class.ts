import { Subject } from 'rxjs';
import { FileProgress } from './file-uploader.service';

/**
 * @see https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/readyState
 */
export enum RequestStatus {
    UNSENT = 0,
    OPENED = 1,
    HEADERS_RECEIVED = 2,
    LOADING = 3,
    DONE = 4,
};

export class FileUploadObject implements FileProgress {

    /** The file to upload */
    public file: File;
    /** File name */
    public name: string;
    /** File type (MIME-Type) */
    public type: string;
    /** Total file-size in bytes */
    public size: number;

    /** Subject of how much % are currently uploaded */
    public progress$ = new Subject<number>();
    /** If the upload has been cancelled */
    public cancelled = false;
    /** If the upload is done (same as status === RequestStatus.DONE) */
    public done = false;
    /** The XMLHttpStatus of the request */
    public status: RequestStatus = RequestStatus.UNSENT;
    /** The error that was encountered during upload */
    public error: Error;
    /** How many bytes have been uploaded */
    public uploaded = 0;
    /** The response from the server */
    public response: any;
    /** The response status code */
    public statusCode: number;
    /** Temporary ID of the file to identify a request */
    public uid: string;
    /** The dimensions of the image (if it is an image to begin with) */
    public get dimensions(): Promise<{ width?: number; height?: number; }> {
        return this.dimensionsPromise;
    }

    /** The actual XMLHttpRequest which has been sent */
    private request: XMLHttpRequest;
    private dimensionsPromise: Promise<{ width?: number; height?: number; }>;
    private lastEmittedValue: number;

    constructor(file: File, request: XMLHttpRequest) {
        this.uid = Math.random().toString(36).substr(2);
        this.name = file.name;
        this.size = file.size;
        this.type = file.type;
        this.request = request;

        if (/^image\/.*/.test(this.type)) {
            this.dimensionsPromise = this.getImageDimensions(file);
        } else {
            this.dimensionsPromise = Promise.resolve({});
        }

        request.onreadystatechange = (event) => this.onReadyStateChange(event);
        request.upload.onprogress = (event) => this.onProgress(event);
        request.upload.onabort = (event) => this.onAbort(event);
        request.upload.onerror = (error) => this.onError(error);
    }

    /**
     * If the file is an image, then we can read its dimensions. This is necessary when uploading a new image, since the
     * GCN API only returns a file object. Therefore, in order to get the image dimensions, we must inspect the file
     * before uploading.
     */
    private getImageDimensions(file: File): Promise<{ width: number; height: number; }> {
        let getDataUrl: Promise<string>;
        let cleanup = () => { };

        // Browser with object URL support don't need to load the full file content
        // into memory, but need to release the URL manually.
        if (typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function') {
            let url = URL.createObjectURL(file);
            getDataUrl = Promise.resolve(url);
            cleanup = () => URL.revokeObjectURL(url);
        } else {
            getDataUrl = new Promise((resolve, reject) => {
                let reader = new FileReader();
                reader.onload = () => resolve(reader.result as string);
                reader.onerror = (ev: any) => reject(ev.error);
                reader.readAsDataURL(file);
            });
        }

        return getDataUrl.then<any>(url => new Promise((resolve, reject) => {
            let img = new Image();
            img.onload = () => {
                resolve({
                    width: img.width,
                    height: img.height,
                });
                cleanup();
            };
            img.onerror = (ev: any) => reject(ev.error);
            img.src = url;
        })).catch(err => {
            cleanup();
            throw err;
        });
    }

    private onProgress(event: ProgressEvent): void {
        this.uploaded = event.loaded;
        this.size = event.total;
        this.emitProgress();
    }

    private onAbort(event: Event): void {
        this.done = this.cancelled = true;
        this.emitProgress();
        this.progress$.complete();
    }

    private onError(event: ProgressEvent): void {
        this.done = true;
        // Unfortunately the ProgressEvent doesn't give us any error details.
        this.error = new Error('XMLHttpRequest error');
        this.progress$.error(this.error);
    }

    private onReadyStateChange(event: Event): void {
        let request = this.request;
        this.status = request.readyState;

        if (request.readyState === RequestStatus.DONE) {
            this.done = true;
            this.statusCode = request.status;

            this.response = undefined;
            let error = this.error;

            // When the server rejects overly large files, the request is not passed to ContentNode,
            // and an HTML error page is returned instead of a JSON response.
            if (request.status === 413 && request.getResponseHeader('Content-Type') === 'text/html') {
                error = new Error('File is too large to upload.');
            } else if (request.response && !error) {
                try {
                    this.response = JSON.parse(request.response);
                } catch (err) {
                    error = err;
                }
            }

            this.emitProgress();
            if (request.status >= 200 && request.status < 300 && !error) {
                this.progress$.complete();
            } else {
                this.error = error || new Error('Server answered with HTTP ' + request.status);
                this.progress$.error(this.error);
            }
        }
    }

    private emitProgress(): void {
        let percent = this.size ? this.uploaded / this.size : 0;
        if (percent !== this.lastEmittedValue) {
            this.progress$.next(this.lastEmittedValue = percent);
        }
    }
}
