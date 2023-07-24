export function iconForItemType(itemType: string, fallback: string = 'help_outline'): string {
    if (typeof itemType !== 'string') {
        return 'Error: Input type is not string';
    }
    switch (itemType.toLowerCase()) {
        case 'file':
        case 'linkedfile':
            return 'insert_drive_file';
        case 'folder':
            return 'folder';
        case 'form':
            return 'list_alt';
        case 'image':
        case 'linkedimage':
            return 'photo';
        case 'page':
        case 'linkedpage':
            return 'subject';
        case 'template':
            return 'dashboard';
        case 'contenttag':
        case 'templatetag':
            return 'code';
        case 'channel':
            return 'input';
        case 'node':
            return 'device_hub';

        default:
            return fallback;
    }
}
