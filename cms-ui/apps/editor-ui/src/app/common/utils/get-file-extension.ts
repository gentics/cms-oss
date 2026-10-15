const COMPRESSION_TYPES = [
    'br',
    'bz2',
    'f',
    'genozip',
    'gz',
    'lz',
    'lz4',
    'lzma',
    'lzo',
    'rz',
    'sfark',
    'sz',
    'xz',
    'z',
    'zst',
];

const NORMALIZATION_MAP = {
    // Images
    jfif: 'jpg',
    jpe: 'jpg',
    jpeg: 'jpg',
    tiff: 'tif',
    // Audio
    aif: 'aiff',
    aifc: 'aiff',
    midi: 'mid',
    // Video
    mpe: 'mpg',
    mpeg: 'mpg',
    qt: 'mov',
    // Documents / Text
    htm: 'html',
    markdown: 'md',
    mkd: 'md',
    text: 'txt',
    yml: 'yaml',
    // Calendar / Contacts
    ical: 'ics',
    icalendar: 'ics',
    vcard: 'vcf',
    // Archives
    zipx: 'zip',
};

/**
 * Returns file extension from file name
 */
export function getFileExtension(fileName: string, normalize: boolean = false): string {
    if (fileName == null || fileName.trim() === '') {
        return '';
    }

    const rawParts = fileName.split('.');
    const parts = rawParts
        .filter((part) => part.trim() !== '')
        .map((part) => part.toLowerCase());

    const lastPart = parts[parts.length - 1];

    if (
        // Files which don't have a extension at all
        rawParts.length === 1
        // Dotfiles which don't have any extension
        || (
            parts.length === 1
            && rawParts.length === 2
            && rawParts[0] === ''
            && rawParts[1] !== ''
        )
    ) {
        return '';
    }

    // If we have a compression type as the last part, then there's a
    // convention to have these appended to the actual file extension.
    // Example: "my-archive.tar.gz" -> "tar.gz"
    // Only apply when we have more parts available.
    if (COMPRESSION_TYPES.includes(lastPart) && parts.length > 2) {
        const ext = parts[parts.length - 2];
        const norm = normalize ? NORMALIZATION_MAP[ext] || ext : ext;
        return `${norm}.${lastPart}`;
    }

    return normalize ? NORMALIZATION_MAP[lastPart] || lastPart : lastPart;
}
