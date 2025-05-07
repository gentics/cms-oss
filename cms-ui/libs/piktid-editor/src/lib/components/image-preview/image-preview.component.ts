import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Coordinates } from '../../common/models';

@Component({
    selector: 'gtxpikt-image-preview',
    templateUrl: './image-preview.component.html',
    styleUrls: ['./image-preview.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImagePreviewComponent {

    /** The URL of the original image. */
    @Input()
    public originalImageUrl: string | null = null;

    /** The URL of the edited image. */
    @Input()
    public editedImageUrl: string | null = null;

    /** The active image. */
    @Input()
    public activeImage: 'original' | 'edited' = 'original';

    @Input()
    public faceIds: number[] = [];

    @Input()
    public facePositions: Record<number, Coordinates> = {};

    /** The event emitter for the active image. */
    @Output()
    public activeImageChange = new EventEmitter<string>();

    onTabChange(id: string): void {
        this.activeImageChange.emit(id);
    }
}
