import { ChangeDetectionStrategy, Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { Image as ImageModel } from '@gentics/cms-models';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';

@Component({
    selector: 'image-thumbnail',
    templateUrl: './image-thumbnail.tpl.html',
    styleUrls: ['./image-thumbnail.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageThumbnailComponent implements OnChanges {
    @Input() image: ImageModel;
    @Input() nodeId: number;
    @Input() width: number;
    @Input() maxHeight: number;
    @Input() showName = false;
    @Input() minHeight: number;
    height: number;

    imageSrc: string;

    @ViewChild('thumbnailImage', { static: true }) thumbnailImage: ElementRef;

    constructor(private resourceUrlBuilder: ResourceUrlBuilder) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['image']) {
            const previous: ImageModel = changes['image'].previousValue || {};
            const current: ImageModel = changes['image'].currentValue || {};
            if (previous.edate !== current.edate) {
                this.imageSrc = this.createImageSrcUrl();
            }
        }
    }

    private createImageSrcUrl(): string {
        const MIN_HEIGHT = this.minHeight ?? 130;
        this.width = Math.round(+this.width || this.image.sizeX);
        const ratio = this.width / this.image.sizeX;
        const imageWidth = Math.min(this.width, this.image.sizeX);
        const imageHeight =
            this.image.sizeX < this.width
                ? this.image.sizeY
                : this.image.sizeY * ratio;
        const changeDate = this.image.edate || this.image.cdate;
        const fileType = this.image.fileType;
        this.height = Math.round(Math.max(MIN_HEIGHT, imageHeight));
        let imageSrcUrl;
        if (!this.image.sizeX) {
            imageSrcUrl = this.resourceUrlBuilder.imageFullsize(this.image.id, this.nodeId, this.image.edate || this.image.cdate);
        } else {
            imageSrcUrl = this.resourceUrlBuilder.imageThumbnail(
                this.image.id,
                imageWidth,
                imageHeight,
                this.nodeId,
                changeDate,
                fileType
            );
        }

        return imageSrcUrl;
    }
}
