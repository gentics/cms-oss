import { AvailableImageFormats, Image } from "@gentics/cms-models";

export function getImageTypeFromFileName(fileName: string): AvailableImageFormats | null {
    const idx = fileName.lastIndexOf('.');

    // If the file has no extension
    if (idx === -1) {
        return null;
    }
    const ext = fileName.substring(idx + 1).toLowerCase();

    switch (ext) {
        // Simple formats which are the file-extensions anyways
        case 'png':
        case 'jpg':
        case 'webp':
        case 'bmp':
        case 'gif':
        case 'tiff':
        case 'wbmp':
            return ext;

        // some alternative file extensions
        case 'jpeg':
            return 'jpg';
        case 'tif':
            return 'tiff';
    }

    return null;
}

export function getImageTypeFromMimeType(mimeType: string): AvailableImageFormats | null {
    switch (mimeType.toLowerCase()) {
        case 'image/bmp':
            return 'bmp';
        case 'image/gif':
            return 'gif';
        case 'image/jpeg':
            return 'jpg';
        case 'image/png':
            return 'png';
        case 'image/tiff':
            return 'tiff'
        case 'image/vdn.wap.wbmp':
            return 'wbmp';
        case 'image/webp':
            return 'webp';
    }

    return null;
}

export function getImageType(image: Image): AvailableImageFormats | null {
    return getImageTypeFromMimeType(image.fileType) ?? getImageTypeFromFileName(image.name);
}
