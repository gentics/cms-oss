import { Directive, HostListener, Input, OnDestroy } from '@angular/core';
import { DragStateTrackerFactoryService } from '../../providers/drag-state-tracker/drag-state-tracker.service';
import { PageFileDragHandlerService } from '../../providers/page-file-drag-handler/page-file-drag-handler.service';
import { getDataTransfer, transferHasFiles } from '../../utils/drag-and-drop';

/**
 * Prevents accidentally dropping files outside of a {@link FileDropAreaDirective}
 */
@Directive({
    selector: '[gtxPreventFileDrop]',
    providers: [PageFileDragHandlerService, DragStateTrackerFactoryService],
})
export class PreventFileDropDirective implements OnDestroy {

    @Input()
    set gtxPreventFileDrop(val: boolean | 'true' | 'false' | 'page') {
        let mode: boolean | 'page' = val === 'page' ? 'page' : (val !== false && val !== 'false');
        if (mode != this.prevent) {
            this.dragHandler.preventFileDropOnPageFor(this, mode === 'page');
            this.prevent = mode;
        }
    }

    prevent: boolean | 'page' = true;

    constructor(private dragHandler: PageFileDragHandlerService) { }

    ngOnDestroy(): void {
        this.dragHandler.preventFileDropOnPageFor(this, false);
    }

    @HostListener('dragenter', ['$event'])
    @HostListener('dragover', ['$event'])
    @HostListener('drop', ['$event'])
    preventAccidentalDrop(event: Event): void {
        if (this.prevent !== true || event.defaultPrevented) { return; }
        let dataTransfer = getDataTransfer(event);
        if (transferHasFiles(dataTransfer)) {
            event.preventDefault();
            dataTransfer.effectAllowed = 'none';
            dataTransfer.dropEffect = 'none';
        }
    }
}
