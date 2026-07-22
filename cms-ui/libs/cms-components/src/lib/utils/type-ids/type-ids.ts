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

export function nameToTypeId(name: string): number {
    switch (name) {
        case 'node': return 10001;
        case 'folder': return 10002;
        case 'template': return 10006;
        case 'page': return 10007;
        case 'file': return 10008;
        case 'image': return 10011;
        case 'contentgroup': return 10031;
        case 'contenttag': return 10111;
        case 'templatetag': return 10112;
        case 'objectag': return 10113;
        default: return null;
    }
}
