import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { ISortableEvent, PageFileDragHandlerService } from '@gentics/ui-core';
import { Subscription, merge as observableMerge } from 'rxjs';
import { map, mapTo } from 'rxjs/operators';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './file-drop-area-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FileDropAreaDemoPage implements OnDestroy {

    @InjectDocumentation('file-drop-area.directive')
    documentation: IDocumentation;

    draggingFileOnThis = false;
    droppedFiles: File[] = [];
    droppedFilesA: any;
    droppedFilesC: any;
    draggingFileOnPage: boolean;
    droppedImages: File[] = [];
    droppedTextFiles: File[] = [];
    rejectedImages: File[] = [];
    rejectedTextFiles: File[] = [];
    reorderableFiles: File[] = [];
    isDisabled = true;
    pageDragHovered: boolean;
    preventOnPage = true;
    preventLocal = false;
    serviceEvents: string[] = [];
    subscription: Subscription;

    constructor(
        public dragdrop: PageFileDragHandlerService,
    ) {
        this.subscription = observableMerge(
            dragdrop.dragEnter.pipe(mapTo('dragEnter')),
            dragdrop.dragStop.pipe(mapTo('dragStop')),
            dragdrop.filesDragged$.pipe(map($event => `filesDragged$ ($event = ${JSON.stringify($event)})`)),
        ).subscribe(eventText => {
            let d = new Date();
            let time = d.toTimeString().split(' ')[0] + (d.getMilliseconds() / 1000).toFixed(3).substr(1);
            this.serviceEvents = this.serviceEvents.concat(`${time}: ${eventText}`);
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    onDropFiles(files: File[]): void {
        this.draggingFileOnThis = false;
        this.droppedFiles.push(...files);
    }

    reorderList(event: ISortableEvent): void {
        this.reorderableFiles = event.sort(this.reorderableFiles) as File[];
    }

    addFilesToReorderableList(files: File[]): void {
        this.reorderableFiles.push(...files);
    }
}
