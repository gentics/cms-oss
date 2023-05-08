import { EventEmitter, Inject, Injectable, InjectionToken, Optional } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { filter, mapTo } from 'rxjs/operators';
import { getDataTransfer, transferHasFiles } from '../../utils/drag-and-drop';
import { matchesMimeType } from '../../utils/matches-mime-type';
import { DragStateTrackerFactoryService, FileDragState } from '../drag-state-tracker/drag-state-tracker.service';

/**
 * A token that can be used to inject a mock into the service
 *
 * @internal
 */
export const PAGE_FILE_DRAG_EVENT_TARGET = new InjectionToken('PAGE_FILE_DRAG_EVENT_TARGET');

/**
 * A helper service that listens for dragenter/dragleave/drop events on the window
 * and tracks when files are dragged into or out of the active tab.
 *
 * When files are accidentally dropped on the tab outside of a drop zone, the drop
 * can be cancelled to prevent the browser from opening/downloading the file.
 * See {@link PreventFileDropDirective} for details.
 */
@Injectable()
export class PageFileDragHandlerService {

    /**
     * Fires when a file is dragged into the current tab, dragged out or dropped.
     *
     * @exmample
     *   class Component {
     *     constructor(private pageDrag: PageFileDragStatusService) { }
     *   }
     *
     *   <ul *ngIf="pageDrag.filesDragged$ | async"> ... </ul>
     */
    filesDragged$: Observable<FileDragState>;

    /**
     * Emits a list when a file is dragged into the current tab.
     * The list contains `{ "type": string }` elements.
     */
    dragEnter: Observable<FileDragState>;

    /**
     * Emits false when a file is dragged out of the current tab.
     */
    dragStop: Observable<boolean>;

    /**
     * Fires when a drop event is prevented.
     */
    dropPrevented = new EventEmitter<void>();

    private internalFilesDragged: FileDragState = [];
    private subscription: Subscription;
    private eventTarget: EventTarget;
    private eventsBound = false;
    private componentsWantingToPreventFileDrop = new Set<any>();
    private preventAccidentalFileDrop = false;

    /**
     * Returns true if a file is dragged over the current page/tab.
     *
     * @example
     *   class Component {
     *     constructor(private pageDragStatus: PageFileDragStatusService) {}
     *   }
     *
     *   <ul *ngIf="pageDragStatus.filesDragged"> ... </ul>
     */
    public get filesDragged(): FileDragState {
        return this.internalFilesDragged;
    }

    /**
     * Returns true if files are dragged over the current tab and any file matches the specified mime type.
     *
     * @example
     *   class Component {
     *     constructor(private pageDragStatus: PageFileDragStatusService) {}
     *   }
     *
     *   <ul *ngIf="pageDragStatus.anyDraggedFileIs('image/*')"> ... </ul>
     */
    public anyDraggedFileIs(allowedTypes: string): boolean {
        return !!(this.internalFilesDragged.length && this.internalFilesDragged.some(file =>
            matchesMimeType(file.type, allowedTypes)));
    }

    /**
     * Returns true if files are dragged over the current page and all files match the specified mime type.
     *
     * @example
     *   class Component {
     *     constructor(private pageDragStatus: PageFileDragStatusService) {}
     *   }
     *
     *   <ul *ngIf="pageDragStatus.allDraggedFilesAre('image/*')"> ... </ul>
     */
    public allDraggedFilesAre(allowedTypes: string): boolean {
        return !!(this.internalFilesDragged.length && this.internalFilesDragged.every(file =>
            matchesMimeType(file.type, allowedTypes)));
    }


    constructor(@Optional() @Inject(PAGE_FILE_DRAG_EVENT_TARGET) eventTarget: any,
        dragState: DragStateTrackerFactoryService) {

        if (eventTarget) {
            this.eventTarget = eventTarget;
        } else if (typeof window === 'object') {
            this.eventTarget = window;
        } else {
            throw new Error('No event target for PageFileDragHandler.');
        }

        this.filesDragged$ = dragState.trackElement(this.eventTarget);
        this.dragEnter = this.filesDragged$.pipe(
            filter(list => list.length > 0),
        );
        this.dragStop = this.filesDragged$.pipe(
            filter(list => list.length === 0),
            mapTo(false),
        );
        this.bindEvents();
    }

    destroy(): void {
        this.unbindEvents();
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = undefined;
        }
        this.componentsWantingToPreventFileDrop = new Set<HTMLElement>();
        this.preventAccidentalFileDrop = false;
    }

    /** @internal */
    bindEvents(): void {
        if (this.eventsBound) { return; }
        this.subscription = this.filesDragged$.subscribe(dragged => this.internalFilesDragged = dragged);
        this.eventTarget.addEventListener('dragenter', this.preventAccidentalDrop, false);
        this.eventTarget.addEventListener('dragover', this.preventAccidentalDrop, false);
        this.eventTarget.addEventListener('drop', this.preventAccidentalDrop, false);
        this.eventsBound = true;
    }

    /** @internal */
    unbindEvents(): void {
        this.eventTarget.removeEventListener('dragenter', this.preventAccidentalDrop, false);
        this.eventTarget.removeEventListener('dragover', this.preventAccidentalDrop, false);
        this.eventTarget.removeEventListener('drop', this.preventAccidentalDrop, false);
        this.eventsBound = false;
    }

    /** @internal */
    preventFileDropOnPageFor(directive: any, prevent: boolean): void {
        if (prevent) {
            this.componentsWantingToPreventFileDrop.add(directive);
        } else {
            this.componentsWantingToPreventFileDrop.delete(directive);
        }
        this.preventAccidentalFileDrop = this.componentsWantingToPreventFileDrop.size > 0;
    }

    private preventAccidentalDrop = (event: DragEvent) => {
        let dataTransfer = getDataTransfer(event);
        if (this.preventAccidentalFileDrop && !event.defaultPrevented && transferHasFiles(dataTransfer)) {
            event.preventDefault();
            dataTransfer.effectAllowed = 'none';
            dataTransfer.dropEffect = 'none';
            if (event.type === 'drop') {
                this.dropPrevented.emit(undefined);
            }
        }
    }

}
