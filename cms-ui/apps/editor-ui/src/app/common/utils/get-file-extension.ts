/**
 * Returns file extension from file name
 */
export function getFileExtension(fileName: string): string {
    const matches = fileName.match(/\.([^\.]+)$/);
    return matches ? matches[1] : '';
}
