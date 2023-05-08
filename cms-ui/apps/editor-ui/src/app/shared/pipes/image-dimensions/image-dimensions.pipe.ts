import {Pipe, PipeTransform} from '@angular/core';

/**
 * Formats image dimensions into string.
 */
@Pipe({ name: 'imagedimensions' })
export class ImageDimensionsPipe implements PipeTransform {
    transform(value: any): string {
        if (value.width && value.height && Number(value.width) > 0 && Number(value.height) > 0) {
            return `${value.width} x ${value.height}`;
        } else {
            return 'common.image_scalable';
        }
    }
}
