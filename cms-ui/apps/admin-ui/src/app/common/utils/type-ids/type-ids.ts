export function typeIdsToName(id: number): string {
    switch (id) {
        case 10001: return 'node';
        case 10002: return 'folder';
        case 10006: return 'template';
        case 10007: return 'page';
        case 10008: return 'file';
        case 10011: return 'image';
        case 10031: return 'contentgroup';
        case 10111: return 'contenttag';
        case 10112: return 'templateTag';
        case 10113: return 'objecttag';
        default: return null;
    }
}
