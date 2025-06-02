import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    SimpleChanges,
} from '@angular/core';

/**
 * Shows a preview of an image supplied as File object.
 */
@Component({
    selector: 'image-preview',
    templateUrl: './image-preview.component.html',
    styleUrls: ['./image-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ImagePreviewComponent implements OnChanges {

    @Input() file: File;

    imageUrl: string;
    loading = false;

    constructor(private changeDetector: ChangeDetectorRef) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.file && this.file) {
            this.updateImageUrl(this.file);
        }
    }

    private updateImageUrl = (file: File) => {
        if (!!this.imageUrl && typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function') {
            URL.revokeObjectURL(this.imageUrl);
        }
        this.imageUrl = null;
        if (file && /^image\/.*/.test(file.type)) {
            this.loading = true;
            if (typeof URL !== 'undefined' && typeof URL.createObjectURL === 'function') {
                this.loading = true;
                this.changeDetector.detectChanges();
                this.imageUrl = URL.createObjectURL(file);
                this.loading = false;
                this.changeDetector.markForCheck();
            } else {
                this.loading = true;
                this.changeDetector.detectChanges();
                const reader = new FileReader();
                reader.onloadend = (e) => {
                    this.imageUrl = reader.result as string;
                    this.loading = false;
                    this.changeDetector.markForCheck();
                }
                reader.readAsDataURL(file);
            }
        }
    }

}
