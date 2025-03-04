import { Directive, ElementRef, EventEmitter, Inject, Input, NgZone, OnDestroy, OnInit, Optional, Output } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { DEFAULT_FILE_DROP_AREA_OPTIONS, FILE_DROPAREA_DRAG_EVENT_TARGET, IDraggedFile, IFileDropAreaOptions } from '../../common';
import { DragStateTrackerFactoryService, FileDragState } from '../../providers/drag-state-tracker/drag-state-tracker.service';
import { PageFileDragHandlerService } from '../../providers/page-file-drag-handler/page-file-drag-handler.service';
import { clientReportsMimeTypesOnDrag, getDataTransfer, transferHasFiles } from '../../utils/drag-and-drop';
import { matchesMimeType } from '../../utils/matches-mime-type';

/**
 * File upload area that accepts files via drag and drop.
 *
 * ```html
 * <gtx-file-drop-area (fileDrop)="files = $event">Upload files via drag & drop</gtx-file-drop-area>
 * <div gtxFileDropArea (fileDrop)="files = $event">Upload files via drag & drop</div>
 * ```
 */
@Directive({
    selector: 'gtx-file-drop-area, [gtxFileDropArea]',
    exportAs: 'gtxFileDropArea',
    providers: [DragStateTrackerFactoryService, PageFileDragHandlerService],
})
export class FileDropAreaDirective implements OnInit, OnDestroy {

    /**
     * Returns true if an accepted file is dragged on the drop area.
     */
    public get dragHovered(): boolean {
        return this._isDraggedOver;
    }

    /**
     * Returns a list of mime types of accepted files dragged over the drop area.
     */
    public get draggedFiles(): FileDragState {
        return this._draggedFiles;
    }

    /**
     * Returns true if an accepted file is dragged on the page.
     */
    public get pageDragHovered(): boolean {
        return this._isPageDraggedOver;
    }

    /**
     * If accepted files are dragged inside the current page / browser tab,
     * returns a list of the dragged file types, `undefined` otherwise.
     */
    public get filesDraggedInPage(): FileDragState {
        return this._filesDraggedInPage;
    }

    /**
     * Sets options of this drop area.
     */
    @Input('gtxFileDropArea') get options(): IFileDropAreaOptions {
        return this._options;
    }
    set options(options: IFileDropAreaOptions) {
        this._options = Object.freeze(Object.assign({}, DEFAULT_FILE_DROP_AREA_OPTIONS, options));
    }

    /**
     * Emits a list when files are dragged over the drop area, `undefined` otherwise.
     * Can be used with AsyncPipe for change detection and subscription handling.
     */
    draggedFiles$: Observable<FileDragState>;

    /**
     * Emits a list when files are dragged over the drop area, `false` otherwise.
     * Can be used with AsyncPipe for change detection and subscription handling.
     */
    filesDraggedInPage$: Observable<FileDragState>;

    /**
     * Fires when a file or files are dragged over the drop area.
     */
    @Output() fileDragEnter = new EventEmitter<FileDragState>();

    /**
     * Fires when a file or files are dragged out of the drop area.
     */
    @Output() fileDragLeave = new EventEmitter<void>();

    /**
     * Fires when a file or files are dropped on the drop area.
     */
    @Output() fileDrop = new EventEmitter<File[]>();

    /**
     * Fires when a file or files which do not match the "accepted" option
     * are dropped on the drop area.
     */
    @Output() fileDropReject = new EventEmitter<File[]>();

    /**
     * Fires when a file or files are dragged into the page.
     */
    @Output() pageDragEnter = new EventEmitter<FileDragState>();

    /**
     * Fires when a file or files is dragged out of the page.
     */
    @Output() pageDragLeave = new EventEmitter<void>();


    private _draggedFiles: IDraggedFile[] = [];
    private _isDraggedOver = false;
    private _isPageDraggedOver = false;
    private _filesDraggedInPage: IDraggedFile[] = [];
    private _options = DEFAULT_FILE_DROP_AREA_OPTIONS;
    private _subscriptions: Subscription[] = [];
    private _eventTarget: EventTarget;


    constructor(elementRef: ElementRef,
        @Optional() @Inject(FILE_DROPAREA_DRAG_EVENT_TARGET) dragEventTarget: any,
        private pageDrag: PageFileDragHandlerService,
        private fileDrag: DragStateTrackerFactoryService,
        zone: NgZone) {

        this._eventTarget = dragEventTarget || elementRef.nativeElement;

        this.draggedFiles$ = fileDrag.trackElement(this._eventTarget).pipe(
            map(files => files.filter(this.accepts)),
        );

        this.filesDraggedInPage$ = pageDrag.filesDragged$.pipe(
            map(files => files.filter(this.accepts)),
        );

        this._subscriptions = [
            this.draggedFiles$.subscribe(files => {
                zone.runGuarded(() => {
                    this._isDraggedOver = files.length > 0;
                    this._draggedFiles = files;
                    if (files.length > 0) {
                        this.fileDragEnter.emit(files);
                    } else {
                        this.fileDragLeave.emit(undefined);
                    }
                });
            }),
            this.filesDraggedInPage$.subscribe(filesInPage => {
                zone.runGuarded(() => {
                    this._isPageDraggedOver = filesInPage.length > 0;
                    this._filesDraggedInPage = filesInPage;
                    if (filesInPage.length > 0) {
                        this.pageDragEnter.emit(filesInPage);
                    } else {
                        this.pageDragLeave.emit(undefined);
                    }
                });
            }),
        ];
    }

    ngOnInit(): void {
        this._eventTarget.addEventListener('dragenter', this.onDragEnterOver);
        this._eventTarget.addEventListener('dragover', this.onDragEnterOver);
        this._eventTarget.addEventListener('drop', this.onDrop);
    }

    ngOnDestroy(): void {
        this._subscriptions.forEach(s => s.unsubscribe());
        this._eventTarget.removeEventListener('dragenter', this.onDragEnterOver);
        this._eventTarget.removeEventListener('dragover', this.onDragEnterOver);
        this._eventTarget.removeEventListener('drop', this.onDrop);
        this.pageDrag.destroy();
    }

    private accepts = (file: {type: string}): boolean => {
        return !clientReportsMimeTypesOnDrag() || matchesMimeType(file.type, this._options.accept);
    }

    private onDragEnterOver = (event: DragEvent) => {
        let transfer = getDataTransfer(event);
        if (!transferHasFiles(transfer)) {
            return;
        }

        if (this._isDraggedOver && !this._options.disabled) {
            transfer.dropEffect = 'copy';
        } else {
            transfer.dropEffect = 'none';
            transfer.effectAllowed = 'none';
        }
        event.preventDefault();
    }

    private onDrop = (event: DragEvent) => {
        let transfer = getDataTransfer(event);
        if (event.defaultPrevented || !transferHasFiles(transfer) || this._options.disabled) {
            return;
        }

        event.preventDefault();
        transfer.dropEffect = 'copy';

        let files = Array.from(transfer.files);
        let acceptedFiles: File[] = [];
        let rejectedFiles: File[] = [];

        // Check if the dropped files match the "accept" option
        for (let file of files) {
            if (matchesMimeType(file.type, this._options.accept)) {
                acceptedFiles.push(file);
            } else {
                rejectedFiles.push(file);
            }
        }
        if (acceptedFiles.length > 0) {
            this.fileDrop.emit(acceptedFiles);
        }
        if (rejectedFiles.length > 0) {
            this.fileDropReject.emit(rejectedFiles);
        }
    }
}
