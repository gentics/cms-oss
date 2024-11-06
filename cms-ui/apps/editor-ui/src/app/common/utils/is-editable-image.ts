import { Image } from '@gentics/cms-models';

const NON_EDITABLE_IMAGE_TYPES = [
    'image/svg+xml',
    'image/gif',
];

/**
 * Returns true if the given image can be edited in the UI
 */
export function isEditableImage(image: Image): boolean {
    if (image.type === 'image') {
        return NON_EDITABLE_IMAGE_TYPES.indexOf(image.fileType) === -1;
    } else {
        return true;
    }
}
