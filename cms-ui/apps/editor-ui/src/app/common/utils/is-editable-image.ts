import { Image } from '../models';

const nonEditableImageTypes = [
    'image/svg+xml',
    'image/gif'
];

/**
 * Returns true if the given image can be edited in the UI
 */
export function isEditableImage(image: Image): boolean {
    if (image.type === 'image') {
        return nonEditableImageTypes.indexOf(image.fileType) === -1;
    } else {
        return true;
    } 
}
