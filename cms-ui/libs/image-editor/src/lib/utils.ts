import {CropRect, ImageTransformParams} from './models';

/**
 * Returns a CropperData object with default values based on the natural dimensions of
 * the supplied image element.
 */
export function getDefaultCropRect(img: HTMLImageElement, params?: ImageTransformParams): CropRect {
    const cropRect = params && params.cropRect;
    return {
        startX: cropRect ? cropRect.startX : 0,
        startY: cropRect ? cropRect.startY : 0,
        width: cropRect ? cropRect.width : img ? img.naturalWidth : 0,
        height: cropRect ? cropRect.height : img ? img.naturalHeight : 0,
    };
}
